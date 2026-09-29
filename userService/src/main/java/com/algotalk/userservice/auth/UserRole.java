package com.algotalk.userservice.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserRole {
    SUPER_ADMIN("ROLE_SUPER_ADMIN"),
    ADMIN("ROLE_ADMIN"),
    ADMIN_MANAGER("ROLE_ADMIN_PENDING"),
    USER("ROLE_USER");

    private final String value;
}
