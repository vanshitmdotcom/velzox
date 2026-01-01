package com.velzox.apimonitor.service;

import com.velzox.apimonitor.dto.CredentialDto;
import com.velzox.apimonitor.entity.Credential;
import com.velzox.apimonitor.entity.Project;
import com.velzox.apimonitor.exception.ApiException;
import com.velzox.apimonitor.exception.DuplicateResourceException;
import com.velzox.apimonitor.exception.ResourceNotFoundException;
import com.velzox.apimonitor.repository.CredentialRepository;
import com.velzox.apimonitor.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Credential Service - Manages encrypted API credentials
 * 
 * SECURITY DESIGN:
 * - All credential values are encrypted before storage
 * - Decrypted values are only available in memory during HTTP requests
 * - API responses contain only masked values (****xxxx)
 * - Credentials are project-scoped for organization
 * 
 * SUPPORTED TYPES:
 * - BEARER_TOKEN: Authorization: Bearer <token>
 * - API_KEY: Custom header with API key
 * - BASIC_AUTH: Base64 encoded username:password
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final ProjectRepository projectRepository;
    private final EncryptionService encryptionService;

    /**
     * Get all credentials for a project
     * 
     * @param projectId Project ID
     * @param userId User ID (for access control)
     * @return List of credentials with masked values
     */
    @Transactional(readOnly = true)
    public List<CredentialDto.Response> getCredentialsForProject(Long projectId, Long userId) {
        log.debug("Fetching credentials for project {}", projectId);

        // Verify project access
        verifyProjectAccess(projectId, userId);

        List<Credential> credentials = credentialRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<Long> inUseIds = credentialRepository.findCredentialIdsInUseByProject(projectId);

        return credentials.stream()
                .map(credential -> toResponse(credential, inUseIds.contains(credential.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get a specific credential
     * 
     * @param credentialId Credential ID
     * @param userId User ID (for access control)
     * @return Credential with masked value
     */
    @Transactional(readOnly = true)
    public CredentialDto.Response getCredential(Long credentialId, Long userId) {
        log.debug("Fetching credential {}", credentialId);

        Credential credential = credentialRepository.findByIdAndOwnerId(credentialId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential", credentialId));

        boolean inUse = credentialRepository.isCredentialInUse(credentialId);
        return toResponse(credential, inUse);
    }

    /**
     * Create a new credential
     * 
     * @param request Credential creation data
     * @param userId User ID (for access control)
     * @return Created credential with masked value
     */
    @Transactional
    public CredentialDto.Response createCredential(CredentialDto.CreateRequest request, Long userId) {
        log.info("Creating credential '{}' in project {}", request.getName(), request.getProjectId());

        // Verify project access
        Project project = verifyProjectAccess(request.getProjectId(), userId);

        // Check for duplicate name
        if (credentialRepository.existsByNameAndProjectId(request.getName(), request.getProjectId())) {
            throw DuplicateResourceException.credentialName(request.getName());
        }

        // Validate type-specific requirements
        validateCredentialRequest(request);

        // Encrypt the credential value
        String encryptedValue = encryptionService.encrypt(request.getValue());
        String encryptedUsername = null;
        
        if (request.getType() == Credential.CredentialType.BASIC_AUTH) {
            encryptedUsername = encryptionService.encrypt(request.getUsername());
        }

        // Create credential
        Credential credential = Credential.builder()
                .name(request.getName())
                .type(request.getType())
                .encryptedValue(encryptedValue)
                .headerName(request.getHeaderName())
                .encryptedUsername(encryptedUsername)
                .description(request.getDescription())
                .project(project)
                .build();

        credential = credentialRepository.save(credential);
        log.info("Credential created: {}", credential.getId());

        return toResponse(credential, false);
    }

    /**
     * Update an existing credential
     * 
     * @param credentialId Credential ID
     * @param request Update data
     * @param userId User ID (for access control)
     * @return Updated credential with masked value
     */
    @Transactional
    public CredentialDto.Response updateCredential(Long credentialId, 
                                                   CredentialDto.UpdateRequest request,
                                                   Long userId) {
        log.info("Updating credential {}", credentialId);

        Credential credential = credentialRepository.findByIdAndOwnerId(credentialId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential", credentialId));

        // Check for duplicate name (if changed)
        if (request.getName() != null && !credential.getName().equals(request.getName())) {
            if (credentialRepository.existsByNameAndProjectId(request.getName(), 
                                                              credential.getProject().getId())) {
                throw DuplicateResourceException.credentialName(request.getName());
            }
            credential.setName(request.getName());
        }

        // Update value if provided
        if (request.getValue() != null && !request.getValue().isEmpty()) {
            credential.setEncryptedValue(encryptionService.encrypt(request.getValue()));
        }

        // Update other fields
        if (request.getHeaderName() != null) {
            credential.setHeaderName(request.getHeaderName());
        }
        if (request.getUsername() != null) {
            credential.setEncryptedUsername(encryptionService.encrypt(request.getUsername()));
        }
        if (request.getDescription() != null) {
            credential.setDescription(request.getDescription());
        }

        credential = credentialRepository.save(credential);
        log.info("Credential updated: {}", credentialId);

        boolean inUse = credentialRepository.isCredentialInUse(credentialId);
        return toResponse(credential, inUse);
    }

    /**
     * Delete a credential
     * 
     * @param credentialId Credential ID
     * @param userId User ID (for access control)
     */
    @Transactional
    public void deleteCredential(Long credentialId, Long userId) {
        log.info("Deleting credential {}", credentialId);

        Credential credential = credentialRepository.findByIdAndOwnerId(credentialId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential", credentialId));

        // Check if credential is in use
        if (credentialRepository.isCredentialInUse(credentialId)) {
            throw new ApiException(
                "Cannot delete credential that is in use by endpoints", 
                HttpStatus.CONFLICT
            );
        }

        credentialRepository.delete(credential);
        log.info("Credential deleted: {}", credentialId);
    }

    /**
     * Get decrypted credential for HTTP request (internal use only)
     * 
     * @param credentialId Credential ID
     * @return Decrypted credential data
     */
    @Transactional(readOnly = true)
    public DecryptedCredential getDecryptedCredential(Long credentialId) {
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential", credentialId));

        String decryptedValue = encryptionService.decrypt(credential.getEncryptedValue());
        String decryptedUsername = null;
        
        if (credential.getEncryptedUsername() != null) {
            decryptedUsername = encryptionService.decrypt(credential.getEncryptedUsername());
        }

        return new DecryptedCredential(
                credential.getType(),
                decryptedValue,
                credential.getHeaderName(),
                decryptedUsername
        );
    }

    /**
     * Decrypted credential data for HTTP requests
     */
    public record DecryptedCredential(
            Credential.CredentialType type,
            String value,
            String headerName,
            String username
    ) {
        /**
         * Get the authorization header value based on credential type
         */
        public String getAuthorizationHeader() {
            return switch (type) {
                case BEARER_TOKEN -> "Bearer " + value;
                case API_KEY -> value;
                case BASIC_AUTH -> "Basic " + Base64.getEncoder()
                        .encodeToString((username + ":" + value).getBytes());
            };
        }

        /**
         * Get the header name to use
         */
        public String getHeaderNameToUse() {
            return switch (type) {
                case BEARER_TOKEN -> "Authorization";
                case API_KEY -> headerName != null ? headerName : "X-API-Key";
                case BASIC_AUTH -> "Authorization";
            };
        }
    }

    /**
     * Verify user has access to project
     */
    private Project verifyProjectAccess(Long projectId, Long userId) {
        return projectRepository.findByIdAndOwnerId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    /**
     * Validate credential request based on type
     */
    private void validateCredentialRequest(CredentialDto.CreateRequest request) {
        switch (request.getType()) {
            case API_KEY:
                if (request.getHeaderName() == null || request.getHeaderName().isEmpty()) {
                    throw new ApiException("Header name is required for API Key credentials", 
                                          HttpStatus.BAD_REQUEST);
                }
                break;
            case BASIC_AUTH:
                if (request.getUsername() == null || request.getUsername().isEmpty()) {
                    throw new ApiException("Username is required for Basic Auth credentials", 
                                          HttpStatus.BAD_REQUEST);
                }
                break;
            default:
                // BEARER_TOKEN requires no additional fields
                break;
        }
    }

    /**
     * Convert credential to response DTO with masked values
     */
    private CredentialDto.Response toResponse(Credential credential, boolean inUse) {
        String maskedValue = encryptionService.mask(
            encryptionService.decrypt(credential.getEncryptedValue())
        );
        
        String maskedUsername = null;
        if (credential.getEncryptedUsername() != null) {
            maskedUsername = encryptionService.mask(
                encryptionService.decrypt(credential.getEncryptedUsername())
            );
        }

        return CredentialDto.Response.from(credential, maskedValue, maskedUsername, inUse);
    }
}
