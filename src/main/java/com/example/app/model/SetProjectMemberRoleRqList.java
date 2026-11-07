package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SetProjectMemberRoleRqList {
    @JsonProperty("project_members")
    private List<SetProjectMemberRoleRq> members;
}
