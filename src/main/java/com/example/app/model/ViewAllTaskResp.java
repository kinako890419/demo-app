package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ViewAllTaskResp {

    @JsonProperty("project_id")
    private Integer projectId;

    @JsonProperty("tasks_list")
    private List<TaskListResp> taskList;

}
