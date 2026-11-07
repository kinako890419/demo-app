package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddTaskTagsReq {

    @JsonProperty("tag_id")
    @NotNull(message = "Tag Id cannot be null")
    private Integer tagId;

}
