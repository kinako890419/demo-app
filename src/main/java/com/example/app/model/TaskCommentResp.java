package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class TaskCommentResp {

    @JsonProperty("comment_id")
    private Integer commentId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("content")
    private String content;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    @JsonProperty("updated_at")
    private Timestamp updatedAt;

}
