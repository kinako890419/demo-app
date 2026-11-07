package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class ProjDetailsResp {

    @JsonProperty("project_id")
    private Integer id;

    @JsonProperty("creator_id")
    private Long creatorId;

    @JsonProperty("creator_name")
    private String creatorName;

    @JsonProperty("project_name")
    private String projectName;

    @JsonProperty("project_description")
    private String description;

    @JsonProperty("project_status")
    private String status;

    @JsonProperty("member_list")
    private List<ProjectUserDetailsResp> projectMembers;

    @JsonProperty("deadline")
    private Timestamp deadline;

    @JsonProperty("project_created_time")
    private Timestamp createdAt;

    @JsonProperty("project_updated_time")
    private Timestamp updatedAt;

//    @JsonProperty("project_deleted")
//    private Boolean isDeleted;
}
