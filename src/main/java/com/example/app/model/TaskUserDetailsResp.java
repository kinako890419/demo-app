package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class TaskUserDetailsResp {

    @JsonProperty("user_id")
    private Long id;

    @JsonProperty("user_email")
    private String email;

    @JsonProperty("user_name")
    private String name;

    @JsonProperty("invited_by")
    private String invitedBy;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    @JsonProperty("updated_at")
    private Timestamp updatedAt;
}
