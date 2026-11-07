package com.example.app.controller;

import com.example.app.common.models.RespMsg;
import com.example.app.security.CustomUserDetails;
import com.example.app.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "8. Task Attachments CRUD", description = "upload, download and delete files")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentSvc;


    @GetMapping("/projects/{projectId}/tasks/{taskId}/attachments/{id}")
    @Operation(summary = "Download the attachments in task by attachment id", description = "Retrieve a attachment associated with a specific task")
    public ResponseEntity<ByteArrayResource> getAttachmentById(@PathVariable(value = "projectId") Integer projectId,
                                                               @PathVariable (value = "taskId") Integer taskId,
                                                               @PathVariable(value = "id") Integer id,
                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        return attachmentSvc.getAttachmentById(projectId, taskId, id, userDetails.getId());
    }

    @PostMapping("/projects/{projectId}/tasks/{taskId}/attachments")
    @Operation(summary = "Add an attachment to a task", description = "Upload an attachment to a specific task")
    public RespMsg addAttachmentToTask(@PathVariable (value = "projectId") Integer projectId,
                                           @PathVariable(value = "taskId") Integer taskId,
                                           @RequestPart("file") MultipartFile file,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return attachmentSvc.addAttachmentToTask(projectId, taskId, file, userDetails.getId());
    }

    @DeleteMapping("/projects/{projectId}/tasks/{taskId}/attachments/{attachmentId}")
    @Operation(summary = "Delete an attachment from a task", description = "Remove an attachment from a specific task")
    public RespMsg deleteAttachmentFromTask(@PathVariable (value = "projectId") Integer projectId,
                                                @PathVariable(value = "taskId") Integer taskId,
                                                @PathVariable(value = "attachmentId") Integer attachmentId,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return attachmentSvc.deleteAttachmentFromTask(projectId, taskId, attachmentId, userDetails.getId());
    }

}
