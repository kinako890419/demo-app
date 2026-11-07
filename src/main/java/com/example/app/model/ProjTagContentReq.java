package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjTagContentReq {

    @JsonProperty("tag_name")
    @NotBlank(message = "Tag name cannot be blank")
    @Size(max = 10, message = "Tag name must be at most 10 characters long")
    private String tagName;

}
