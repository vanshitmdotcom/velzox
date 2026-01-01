package com.velzox.apimonitor.controller;

import com.velzox.apimonitor.dto.ApiResponse;
import com.velzox.apimonitor.dto.CredentialDto;
import com.velzox.apimonitor.service.CredentialService;
import com.velzox.apimonitor.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Credential Controller - Manages encrypted API credentials
 * 
 * SECURITY:
 * - Credential values are NEVER returned in responses
 * - Only masked values (****xxxx) are shown
 * - All values are encrypted at rest
 * 
 * ENDPOINTS:
 * - GET /api/v1/credentials?projectId={id} - List credentials for a project
 * - GET /api/v1/credentials/{id} - Get credential details (masked)
 * - POST /api/v1/credentials - Create a new credential
 * - PUT /api/v1/credentials/{id} - Update a credential
 * - DELETE /api/v1/credentials/{id} - Delete a credential
 */
@RestController
@RequestMapping("/v1/credentials")
@RequiredArgsConstructor
@Slf4j
public class CredentialController {

    private final CredentialService credentialService;
    private final UserService userService;

    /**
     * Get all credentials for a project
     * 
     * Returns credentials with masked values only.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CredentialDto.Response>>> getCredentials(
            @RequestParam Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        List<CredentialDto.Response> credentials = credentialService.getCredentialsForProject(
                projectId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(credentials));
    }

    /**
     * Get a specific credential by ID
     * 
     * Returns credential with masked value only.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CredentialDto.Response>> getCredential(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        CredentialDto.Response credential = credentialService.getCredential(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success(credential));
    }

    /**
     * Create a new credential
     * 
     * The value is encrypted before storage.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CredentialDto.Response>> createCredential(
            @Valid @RequestBody CredentialDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        CredentialDto.Response credential = credentialService.createCredential(request, userId);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Credential created", credential));
    }

    /**
     * Update an existing credential
     * 
     * If value is provided, it will be re-encrypted.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CredentialDto.Response>> updateCredential(
            @PathVariable Long id,
            @Valid @RequestBody CredentialDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        CredentialDto.Response credential = credentialService.updateCredential(id, request, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Credential updated", credential));
    }

    /**
     * Delete a credential
     * 
     * Cannot delete if credential is in use by endpoints.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCredential(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        credentialService.deleteCredential(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Credential deleted"));
    }
}
