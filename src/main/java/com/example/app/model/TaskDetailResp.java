package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class TaskDetailResp {

    /** more detailed than TaskListResp */

    @JsonProperty("task_id")
    private Integer taskId;

    @JsonProperty("task_name")
    private String taskName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("creator_name")
    private String creatorName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    @JsonProperty("member_lists")
    private List<TaskUserDetailsResp> memberLists;

    @JsonProperty("task_attachments")
    private List<TaskAttachmentResp> taskAttachments;

    @JsonProperty("task_comments")
    private List<TaskCommentResp> taskComments;

    @JsonProperty("tags")
    private List<TagsResp> tags;

    @JsonProperty("updated_at")
    private Timestamp updatedAt;

    @JsonProperty("deadline")
    private Timestamp deadline;

    @JsonProperty("is_editable")
    private Boolean canEdit;

    @JsonProperty("polling")
    private Boolean polling = true;
}
