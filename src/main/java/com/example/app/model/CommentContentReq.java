package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentContentReq {

//    @JsonProperty("project_id")
//    @NotNull(message = "Project ID cannot be empty")
//    private Integer projectId;
//
//    @JsonProperty("task_id")
//    @NotNull(message = "Task ID cannot be empty")
//    private Integer taskId;

    @JsonProperty("content")
    @NotBlank(message = "Comment content cannot be blank")
    @Size(max = 250, message = "Comment content cannot exceed 250 characters")
    private String content;

}
