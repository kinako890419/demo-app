package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditTaskReq {

    /** 沒有限制 Not blank */

    @JsonProperty("task_name")
    @Size(max=50, message = "Task name must be less than 50 chars")
    private String taskName;

    @JsonProperty("task_description")
    @Size(max=250, message = "Task description must be less than 250 chars")
    private String description;

    @JsonProperty("status")
    @Size(max=20, message = "Status must be less than 20 chars")
    private String status;

    /** Will be parsed in task service */
    @JsonProperty("task_deadline")
    private String deadline;

}
