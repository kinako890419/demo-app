package com.example.app.common.enums;

import lombok.Getter;

@Getter
public enum UserRolesEnums {

    PROJECT_OWNER("OWNER"),
    PROJECT_USER("USER"),
    USER("USER"),
    ADMIN("ADMIN"),
    DELETED_USER("DELETED_USER");

    private final String role;

    UserRolesEnums(String role) {
        this.role = role;
    }

}
