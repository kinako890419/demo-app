package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditProjectReq {

    @JsonProperty("project_name")
    @Size(max = 50, message = "Project name must be less than 50 chars")
    private String projectName;

    @JsonProperty("project_description")
    @Size(max = 250, message = "Project description must be less than 50 chars")
    private String description;

    @JsonProperty("project_status")
    @Size(max = 20, message = "Status must be less than 20 characters")
    private String statusName;

    @JsonProperty("deadline")
    private String deadline;

}
