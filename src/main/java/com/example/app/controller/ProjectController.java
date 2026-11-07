package com.example.app.controller;

import com.example.app.model.*;
import com.example.app.security.CustomUserDetails;
import com.example.app.common.models.RespMsg;
import com.example.app.service.ProjectService;
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
@Tag(name = "Project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping("/projects")
    @Operation (summary = "ADMIN or users retrieve a list of projects",
            description = """
                    User get all personal projects, ADMIN can get all projects in DB
                    - page <= 100, starts from 1
                    - page size <= 50, default 5
                    """)
    @Tag(name = "3. Project CRUD")
    @Tag(name = "ADMIN")
    public List<ProjDetailsResp> viewAllProjects(
            @Parameter(schema = @Schema(allowableValues = {"createdAt", "updatedAt", "projectName", "deadline"}))
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,

            @Parameter(schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(required = false, defaultValue = "asc") String order,

            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "5") Integer pageSize,

            @Parameter(schema = @Schema(allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED"}))
            @RequestParam(required = false) String status,

            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.viewAllProjects(sortBy, order, page, pageSize,
                status, userDetails.getId(), userDetails.getUser().getRole());
    }

    @GetMapping("/projects/{id}")
    @Operation(summary = "Get project details by project id",
            description = """
                    Check project details by its ID (id, name, description, status, members list, create/update time)
                    - Only project members (or ADMIN account) can view the project details.
                    - ADMIN can view all projects' details by project ID.
                    """)
    @Tag(name = "ADMIN")
    @Tag(name = "3. Project CRUD")
    public ProjDetailsResp getProjectById(@PathVariable("id") Integer projectId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.getProjByProjectId(projectId, userDetails.getId(), userDetails.getUser().getRole());
    }

    @PostMapping("/projects")
    @Tag(name = "3. Project CRUD")
    @Operation (summary = "Create a new project",
            description = """
                    Create a new project after user login.
                    - the user = project creator (project OWNER by default)
                    - Set the name, description, and status of the project.
                    - if status leaved blank, set to PENDING by default.
                    
                    restrictions:
                    - project name <= 50 chars
                    - project description <= 250 chars
                    - status name: PENDING, IN_PROGRESS, COMPLETED (ignore case)
                    """)
    public RespMsg createProject(@Valid @RequestBody CreateProjectReq req,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.createProject(req, userDetails.getId());
    }

    @PostMapping("/projects/{id}/users")
    @Tag(name = "4. Project Members CRUD")
    @Operation (summary = "Invite a user to the project and set user role",
            description = """
                    Create a new project first by (POST /projects), then invite users to the project by user ID.
                    - The project needs to be **not COMPLETED** to invite users.
                    - Can invite a lists of users and set their roles in the project in one request.
                    - Only project owner can add users to project by user ID.
                    - Set user's role in the project (project OWNER or USER).
                    - If the user to invite account is an available USER, any project owner can invite them
                    2. Check if the user to invite is already in the project
                    - return duplicated error if the user is already in the project
                    """)
    public RespMsg assignProjectMember(@PathVariable Integer id,
                                         @Valid @RequestBody SetProjectMemberRoleRqList req,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {

        return projectService.assignProjectMember(id, req, userDetails.getId());
    }

    @PatchMapping("/projects/{id}")
    @Operation(summary = "Update project name, status and description",
            description = """
                    Edit project name, description, and status.
                    - Data will not change if the column left null.
                    - Only project owner can edit the project.
                    """)
    @Tag(name = "3. Project CRUD")
    public RespMsg editProject(@PathVariable Integer id, @Valid @RequestBody EditProjectReq req,
                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.editProject(id, req, userDetails.getId());
    }

    @PatchMapping("/projects/{id}/users")
    @Tag(name = "4. Project Members CRUD")
    @Operation(summary = "Set project role of a user in the project.",
            description = """
                    - Only project owner can set user role in project.
                        - Owners and the creator can upgrade or downgrade users' roles.
                        - Owner can downgrade other owners to users, except the project creator.
                    - Find user by userId in the request body, cannot change if user account is deleted.
                    - Cannot change role of the project creator. (creator 預設是 owner 不能改)
                    """)
    public RespMsg setProjectUserRole(@PathVariable Integer id,
                                          @Valid @RequestBody SetProjectMemberRoleRq req,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.setProjectUserRole(id, req, userDetails.getId());
    }

    @DeleteMapping("/projects/{id}/users/{userId}")
    @Tag(name = "4. Project Members CRUD")
    @Operation(summary = "Remove user from the project",
            description = """
                    1. Creator and owner can remove all members except the creator
                    2. Everyone (project OWNER, USER) can exit the project by themselves except the project creator.
                    """)
    public RespMsg exitProject(@PathVariable Integer id, @PathVariable Long userId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.exitProject(id, userId, userDetails.getId());
    }

    @DeleteMapping("/projects/{id}")
    @Operation(summary = "Delete project",
            description = """
                    - Delete a project by its project ID.
                    - Only *project creator* can delete the project.
                    """)
    @Tag(name = "3. Project CRUD")
    public RespMsg deleteProject(@PathVariable Integer id,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.deleteProject(id, userDetails.getId());
    }

    @GetMapping("/projects/{id}/tags")
    @Tag(name = "Tags")
    @Operation(summary = "Get all tags in the project",
            description = """
                    - Get all tags in the project by project ID.
                    - Only project members can view the tags in the project.
                    """)
    public ProjectTagsListResp getProjectTags(@PathVariable Integer id,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.getProjectTags(id, userDetails.getId());
    }

    @PostMapping("/projects/{id}/tags")
    @Tag(name = "Tags")
    @Operation(summary = "Add a tag to the project",
            description = """
                    - Create a tag in the project.
                    - Project members can create tags and assign tags to tasks by tag ID.
                    - Tag name must be unique in the project.
                    """)
    public RespMsg addProjectTag(@PathVariable Integer id,
                                     @Valid @RequestBody ProjTagContentReq req,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.addProjectTag(id, req, userDetails.getId());
    }

    @PatchMapping("/projects/{id}/tags/{tagId}")
    @Tag(name = "Tags")
    @Operation(summary = "Edit a tag in the project",
            description = """
                    - Edit a tag by its ID.
                    - Project members can edit tags.
                    - Tag name must be unique in the project.
                    - tasks 上的 tag 內容也會更新
                    """)
    public RespMsg editProjectTag(@PathVariable Integer id,
                                      @PathVariable Integer tagId,
                                      @Valid @RequestBody ProjTagContentReq req,
                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.editProjectTag(id, tagId, req, userDetails.getId());
    }

    @DeleteMapping("/projects/{id}/tags/{tagId}")
    @Tag(name = "Tags")
    @Operation(summary = "Delete a tag from the project",
            description = """
                    - Delete a tag by its ID.
                    - Project members can delete tags.
                    - Remove tag that is assigned to tasks when the tag is deleted from the project.
                    """)
    public RespMsg deleteProjectTag(@PathVariable Integer id,
                                        @PathVariable Integer tagId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return projectService.deleteProjectTag(id, tagId, userDetails.getId());
    }

}
