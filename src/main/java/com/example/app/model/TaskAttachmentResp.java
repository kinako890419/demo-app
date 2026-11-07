package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class TaskAttachmentResp {

    @JsonProperty("attachment_id")
    private Integer id;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("creator_name")
    private String creatorName;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    @JsonProperty("updated_at")
    private Timestamp updatedAt;

}
