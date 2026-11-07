package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class UserProfileResp {

    @JsonProperty("user_id")
    private Long id;

    @JsonProperty("user_email")
    private String email;

    @JsonProperty("user_name")
    private String name;

    @JsonProperty("user_role")
    private String role;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

}
