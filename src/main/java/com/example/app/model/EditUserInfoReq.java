package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditUserInfoReq {

    @JsonProperty("user_name")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @JsonProperty("user_email")
    @Email(message = "Email should be valid")
    @Size(max = 50, message = "Email must be less than 50 characters")
    private String email;

    @JsonProperty("is_admin")
    private Boolean isAdmin;

}
