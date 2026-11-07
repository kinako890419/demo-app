package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaskResp {
    @JsonProperty("task_id")
    private Integer taskId;
    @JsonProperty("task_name")
    private String taskName;
    @JsonProperty("status")
    private String status = "RUNNING";
}
