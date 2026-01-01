package com.velzox.apimonitor.exception;

import org.springframework.http.HttpStatus;

/**
 * Resource Not Found Exception - Thrown when a requested resource doesn't exist
 * 
 * Examples:
 * - Endpoint with given ID not found
 * - Project not found
 * - Credential not found
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s not found with ID: %d", resourceType, id), 
              HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found: %s", resourceType, identifier), 
              HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
