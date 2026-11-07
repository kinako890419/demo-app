package com.example.app.controller;

import com.example.app.model.*;
import com.example.app.common.models.RespMsg;
import com.example.app.security.CustomUserDetails;
import com.example.app.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class TaskController {

    @Autowired
    TaskService taskService;


    /**
     * 列出 project 當中的所有 tasks
     *
     * @param projectId Project ID
     * @param status Project Status name
     * @param sortBy Sort project by
     * @param order Desc or Asc order, default 由小到大
     * @param userDetails user
     * @return Response
     */
    @GetMapping("/projects/{projectId}/tasks")
    @Tag(name = "5. Task CRUD")
    @Operation (summary = "Get tasks list by project ID",
            description = """
                    Retrieve all tasks associated with a specific project
                    - Project OWNER (and creator), USER can view tasks in the project.
                    - Preview of tasks (no attachments, comments)
                    """)
    public ViewAllTaskResp viewTasksByProjId(
            @PathVariable(value = "projectId") Integer projectId,

            @Parameter(schema = @Schema(allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED"}),
                    allowEmptyValue = true)
            @RequestParam(required = false) String status,

            @Parameter(schema = @Schema(allowableValues = {"createdAt", "updatedAt", "taskName", "deadline"}),
                    allowEmptyValue = true)
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,

            @Parameter(schema = @Schema(allowableValues = {"asc", "desc"}),
                    allowEmptyValue = true)
            @RequestParam(required = false, defaultValue = "asc") String order,

            @Parameter(description = """
                    // TODO: ?
                    > 用n個Tag filter project tasks, task 上有其中一個 tag 就會被列出
                    - /projects/{projectId}/tasks?tagIds=1,2
                        - /projects/{projectId}/tasks?sortBy=createdAt&order=desc&tagIds=1,2...
                    - /projects/{projectId}/tasks?tagId=1&tagId=2 (&match=any)
                        - /projects/{projectId}/tasks?sortBy=createdAt&order=desc&tagId=1&tagId=2... (&match=any)
                    """,
                    allowEmptyValue = true)
            @RequestParam(name = "tagIds", required = false) String tagIdListParam,

            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.viewTasksByProjId(projectId, tagIdListParam, status, sortBy, order, userDetails.getId());
    }

    /**
     * 查看 task 的詳細內容 (Description, deadline, attachment, member...)
     *
     * @param taskId Task ID
     * @param userDetails user
     * @return Response
     */
    @GetMapping("/projects/{projectId}/tasks/{taskId}")
    @Tag(name = "5. Task CRUD")
    @Tag(name = "8. Task Attachments CRUD")
    @Operation(summary = "Get task by ID",
            description = """
           <important> run this method in a loop to check the status of a specific task by its ID, keep polling until it shows either completed or error, call 15 times in a row.
           """)
    public TaskDetailResp checkTaskStatus(@PathVariable(value = "projectId") Integer projectId,
                                          @PathVariable(value = "taskId") Integer taskId,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.viewTaskDetails(projectId, taskId, userDetails.getId());
    }

    @PostMapping("/projects/{projectId}/tasks")
    @Tag(name = "5. Task CRUD")
    @Operation(summary = "Create a new task in a project",
            description = """
                    <important> ** follow the instructions**:
                    - after a task created, you MUST **keep polling** to check its status until it shows either completed or error
                    - start polling no matter what status the newly created task shows
                    - if you don't do the polling, the task will time out and when you try to check the status later, you'll get an InvalidTaskId error even if the task did finish successfully and your units will still be consumed.
                    - polling the status of a task by calling `checkTaskStatus` method
                    """)
    public TaskResp createTask (@PathVariable Integer projectId, @Valid @RequestBody AddNewTaskReq req,
                                @AuthenticationPrincipal CustomUserDetails userDetails) {

        return taskService.addTask(projectId, req, userDetails.getId());

    }

    @PatchMapping("/projects/{projectId}/tasks/{taskId}")
    @Tag(name = "5. Task CRUD")
    @Operation (summary = "Update a task data", description = "Update the details of a specific task")
    public RespMsg updateTask(@PathVariable(value = "projectId") Integer projectId,
                                  @PathVariable(value = "taskId") Integer taskId,
                                  @Valid @RequestBody EditTaskReq req,
                                  @AuthenticationPrincipal CustomUserDetails userDetails) {

        return taskService.updateTask(projectId, taskId, req, userDetails.getId());
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}")
    @Tag(name = "5. Task CRUD")
    @Operation(summary = "Delete a task", description = "Delete a specific task by its ID")
    public RespMsg deleteTask(@PathVariable(value = "projectId") Integer projectId,
                                  @PathVariable(value = "taskId") Integer taskId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.deleteTask(projectId, taskId, userDetails.getId());
    }

    @PostMapping("/projects/{projectId}/tasks/{taskId}/users")
    @Tag(name = "6. Task Members (task users) CRUD")
    @Operation(summary = "Add a member to the task",
            description = "Project owner or task user assign a project user to the task")
    public RespMsg assignTaskMember(@PathVariable (value = "projectId") Integer projectId,
                                        @PathVariable(value = "taskId") Integer taskId,
                                        @Valid @RequestBody List<AssignTaskMemberReq> req,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.assignTaskMember(projectId, taskId, req, userDetails.getId());
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}/users/{userId}")
    @Tag(name = "6. Task Members (task users) CRUD")
    @Operation(summary = "Remove a member from a task",
            description = """
                    - Project owner or task user remove a project user from the task
                    - cannot remove the task creator from the task
                    """)
    public RespMsg removeUserFromTask(@PathVariable (value = "projectId") Integer projectId,
                                        @PathVariable(value = "taskId") Integer taskId,
                                        @PathVariable(value = "userId") Long userId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.removeUserFromTask(projectId, taskId, userId, userDetails.getId());
    }

    @PostMapping("/projects/{projectId}/tasks/{taskId}/comments")
    @Tag(name = "7. Task Comments CRUD")
    @Operation(summary = "Add a comment to a task",
            description = """
                    Post a comment on a specific task
                    - comments are limited to 250 characters
                    """)
    public RespMsg addCommentToTask(@PathVariable (value = "projectId") Integer projectId,
                                        @PathVariable(value = "taskId") Integer taskId,
                                        @Valid @RequestBody CommentContentReq req,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.addCommentToTask(projectId, taskId, req, userDetails.getId());
    }

    @PatchMapping("/projects/{projectId}/tasks/{taskId}/comments/{commentId}")
    @Tag(name = "7. Task Comments CRUD")
    @Operation(summary = "Edit a comment in a task",
            description = """
                    Comment creator modify an existing comment in a specific task
                    - Cannot update the comment if not the creator, or the project/task is COMPLETED
                    """)
    public RespMsg updateCommentInTask(@PathVariable (value = "projectId") Integer projectId,
                                           @PathVariable(value = "taskId") Integer taskId,
                                           @PathVariable(value = "commentId") Integer commentId,
                                           @Valid @RequestBody CommentContentReq req,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.updateCommentInTask(projectId, taskId, commentId, req, userDetails.getId());
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}/comments/{commentId}")
    @Tag(name = "7. Task Comments CRUD")
    @Operation(summary = "Delete a comment from a task",
            description = "Comment creator, project owners and task users can remove the comment from a specific task")
    public RespMsg deleteCommentFromTask(@PathVariable (value = "projectId") Integer projectId,
                                             @PathVariable(value = "taskId") Integer taskId,
                                             @PathVariable(value = "commentId") Integer commentId,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        return taskService.deleteCommentFromTask(projectId, taskId, commentId, userDetails.getId());
    }

    @PostMapping("/projects/{projectId}/tasks/{taskId}/tags")
    @Operation(summary = "Add a tag to a task",
            description = "Project owner or task user add an existing tag to a specific task")
    @Tag(name = "Tags")
    public RespMsg addTagToTask(@PathVariable (value = "projectId") Integer projectId,
                                    @PathVariable(value = "taskId") Integer taskId,
                                    @Valid @RequestBody AddTaskTagsReq req,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {

        return taskService.addTagToTask(projectId, taskId, req, userDetails.getId());
    }

    @DeleteMapping ("/projects/{projectId}/tasks/{taskId}/tags/{tagId}")
    @Operation(summary = "Delete a tag from the task",
            description = "Remove a tag from a specific task")
    @Tag(name = "Tags")
    public RespMsg deleteTagFromTask(@PathVariable(value = "projectId") Integer projectId,
                                         @PathVariable(value = "taskId") Integer taskId,
                                         @PathVariable(value = "tagId") Integer tagId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {

        return taskService.deleteTagFromTask(projectId, taskId, tagId, userDetails.getId());
    }

}
