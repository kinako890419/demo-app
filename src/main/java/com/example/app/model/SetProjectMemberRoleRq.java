package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetProjectMemberRoleRq {

    @JsonProperty("user_id")
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    /** OWNER, USER */
    @JsonProperty("user_role")
    @Size(max = 20, message = "User role must be between 1 and 20 characters")
    @NotBlank(message = "User role cannot be blank")
    private String userRole;

}
