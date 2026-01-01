package com.velzox.apimonitor.exception;

import org.springframework.http.HttpStatus;

/**
 * Duplicate Resource Exception - Thrown when attempting to create a duplicate resource
 * 
 * Examples:
 * - User with email already exists
 * - Project with same name already exists
 * - Credential with same name in project
 */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }

    public DuplicateResourceException(String resourceType, String field, String value) {
        super(String.format("%s with %s '%s' already exists", resourceType, field, value), 
              HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }

    public static DuplicateResourceException email(String email) {
        return new DuplicateResourceException("User", "email", email);
    }

    public static DuplicateResourceException projectName(String name) {
        return new DuplicateResourceException("Project", "name", name);
    }

    public static DuplicateResourceException credentialName(String name) {
        return new DuplicateResourceException("Credential", "name", name);
    }
}
