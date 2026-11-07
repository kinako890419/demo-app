package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class TaskListResp {

    @JsonProperty("task_id")
    private Integer taskId;

    @JsonProperty("task_name")
    private String taskName;

    @JsonProperty("creator")
    private String creator;

    @JsonProperty("task_description")
    private String description;

    @JsonProperty("status")
    private String status;

    @JsonProperty("task_deadline")
    private Timestamp deadline;

    @JsonProperty("member_list")
    private List<TaskUserDetailsResp> taskMembers;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    @JsonProperty("updated_at")
    private Timestamp updatedAt;

//    @JsonProperty("is_deleted")
//    private Boolean isDeleted;

    @JsonProperty("is_editable")
    private Boolean canEdit;

    @JsonProperty ("polling")
    private Boolean polling = true;

}
