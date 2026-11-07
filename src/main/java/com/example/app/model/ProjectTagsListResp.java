package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ProjectTagsListResp {

    @JsonProperty("project_id")
    private Integer projectId;

    @JsonProperty("tags")
    private List<TagsResp> tags;

}
