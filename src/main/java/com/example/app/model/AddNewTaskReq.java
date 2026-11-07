package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddNewTaskReq {

    @JsonProperty("task_name")
    @NotBlank(message = "task name cannot be blank")
    @Size(max=50, message = "Task name must be less than 50 chars")
    private String taskName;

    @JsonProperty("task_description")
    @Size(max=250, message = "Task description must be less than 250 chars")
    private String description;

    @JsonProperty("task_status")
    @Size(max=20, message = "Status must be less than 20 chars")
    private String status;

    /** Will be parsed in task service */
    @JsonProperty("task_deadline")
    @NotBlank(message = "Deadline cannot be blank")
    private String deadline;

    @JsonProperty("polling")
    @Schema(description = "Check task status by polling")
    private Boolean polling = true;

}
