package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class TagsResp {

    @JsonProperty("tag_id")
    private Integer tagId;

    @JsonProperty("tag_name")
    private String tagName;

    @JsonProperty("creator")
    private String creator;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    @JsonProperty("updated_at")
    private Timestamp updatedAt;

}
