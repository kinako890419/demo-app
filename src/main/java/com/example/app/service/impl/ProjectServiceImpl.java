package com.example.app.service.impl;

import com.example.app.model.*;
import com.example.app.common.models.RespMsg;
import com.example.app.entity.*;
import com.example.app.common.enums.ProjectTaskEnums;
import com.example.app.common.enums.ResponseStatusEnums;
import com.example.app.common.enums.UserRolesEnums;
import com.example.app.common.exception.DataNotFoundException;
import com.example.app.common.exception.DeleteFailException;
import com.example.app.common.exception.DuplicatedDataException;
import com.example.app.common.exception.InsertionFailException;
import com.example.app.repository.*;
import com.example.app.service.ProjectService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private ProjectMemberRepo projectMemberRepo;

    @Autowired
    private StatusRepository statusRepo;

    @Autowired
    private UsersAccountRepository userRepo;

    @Autowired
    private TagsRepository tagRepo;


    @Override
    public List<ProjDetailsResp> viewAllProjects(String sortBy, String order,
                                                 Integer page, Integer pageSize,
                                                 String statusName, Long userId, String userRole) {

        Set<String> allowedFields = Set.of("createdAt", "updatedAt", "projectName", "deadline");

        String validSortBy = allowedFields.contains(sortBy) ? sortBy : "createdAt";

        Sort sort = "desc".equalsIgnoreCase(order)
                ? Sort.by(validSortBy).descending()
                : Sort.by(validSortBy).ascending();

        Pageable pageable = ObjectUtils.isEmpty(page) ? Pageable.unpaged(sort) :
                PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(pageSize, 1), 50), sort);

        Integer statusId = StringUtils.isBlank(statusName) ? null :
                statusRepo.findByName(statusName.toUpperCase()).map(StatusEntity::getId).orElse(null);

        List<ProjectsEntity> projects =
                StringUtils.equalsIgnoreCase(userRole, UserRolesEnums.ADMIN.getRole())
                        ? projectRepo.findAllProjectsForAdmin(statusId, pageable).toList()
                        : projectRepo.findProjectsForUser(userId, statusId, pageable).toList();

        List<ProjDetailsResp> respProjList = new ArrayList<>();

        for (ProjectsEntity project : projects) {
            ProjDetailsResp respProj = new ProjDetailsResp();

            respProj.setId(project.getId());
            if (ObjectUtils.isEmpty(project.getCreator())) {
                respProj.setCreatorId(null);
                respProj.setCreatorName(UserRolesEnums.DELETED_USER.getRole());
            } else {
                respProj.setCreatorId(project.getCreator().getId());
                respProj.setCreatorName(project.getCreator().getUsername());
            } // project creator cannot be deleted by ADMIN
            respProj.setProjectName(project.getProjectName());
            respProj.setDescription(project.getDescription());
            respProj.setStatus(project.getStatus().getName());
            respProj.setDeadline(project.getDeadline());
            respProj.setCreatedAt(project.getCreatedAt());
            respProj.setUpdatedAt(project.getUpdatedAt());

            // Fetch project members
            respProj.setProjectMembers(fetchProjectMembers(project.getId()));

            respProjList.add(respProj);
        }

        return respProjList;
    }

    @Override
    public ProjDetailsResp getProjByProjectId(Integer projId, Long userId, String userRole) {

        ProjectsEntity project = projectRepo.findById(projId)
                .orElseThrow(() -> new DataNotFoundException("Project not found with ID: " + projId));

        // Only ADMIN can view all projects' details even if the ADMIN is not a project member
        if (!projectMemberRepo.existsByProjectIdAndUserId(projId, userId) &&
                !StringUtils.equals(userRole, UserRolesEnums.ADMIN.getRole())) {
            throw new DataNotFoundException("Project not found with ID: " + projId);
        }

        ProjDetailsResp resp = new ProjDetailsResp();

        resp.setId(project.getId());
        resp.setProjectName(project.getProjectName());
        if (ObjectUtils.isEmpty(project.getCreator())) {
            resp.setCreatorId(null);
            resp.setCreatorName(UserRolesEnums.DELETED_USER.getRole());
        } else {
            resp.setCreatorId(project.getCreator().getId());
            resp.setCreatorName(project.getCreator().getUsername());
        }
        resp.setDescription(project.getDescription());
        resp.setStatus(project.getStatus().getName());
        resp.setDeadline(project.getDeadline());
        resp.setCreatedAt(project.getCreatedAt());
        resp.setUpdatedAt(project.getUpdatedAt());

        // Fetch project members
        resp.setProjectMembers(fetchProjectMembers(project.getId()));

        return resp;
    }

    @Override
    @Transactional
    public RespMsg createProject(CreateProjectReq req, Long userId) {

        ProjectsEntity project = new ProjectsEntity();

        project.setCreator(userRepo.findById(userId).orElseThrow(
                () -> new DataNotFoundException("User not found with ID: " + userId)
        ));
        project.setProjectName(req.getProjectName());
        project.setDescription(req.getDescription());
        project.setDeadline(parseDeadline(req.getDeadline()));
        project.setIsDeleted(false); // default not deleted

        if (StringUtils.isBlank(req.getStatusName())) {
            project.setStatus(statusRepo.findByName(ProjectTaskEnums.PENDING.getStatusName()).orElse(null));
        } else {
            project.setStatus(statusRepo.findByName(req.getStatusName().toUpperCase())
                    .orElseThrow(() -> new DataNotFoundException("Status not found with name: " + req.getStatusName())));
        }

        projectRepo.save(project);

        ProjectMembersEntity projOwner = new ProjectMembersEntity();

        projOwner.setProject(project);
        projOwner.setUser(project.getCreator());
        projOwner.setInvitedBy(project.getCreator());
        projOwner.setProjectRole(UserRolesEnums.PROJECT_OWNER.getRole()); // Set user that created the project as owner
        projOwner.setIsDeleted(false);

        projectMemberRepo.save(projOwner);

        return createSuccessResponse("Project created");
    }

    @Override
    @Transactional
    public RespMsg assignProjectMember(Integer projId, SetProjectMemberRoleRqList req, Long userId) {

        ProjectsEntity proj = projectRepo.findById(projId).orElseThrow(
                () -> new DataNotFoundException("Project not found with ID: " + projId)
        );

        // 檢查 Status COMPLETED
        // Check if the user owns the project
        if (StringUtils.equals(proj.getStatus().getName(),  ProjectTaskEnums.COMPLETED.getStatusName())
                || !checkProjOwner(projId, userId)) {
            throw new InsertionFailException("Cannot invite users to this project");
        }

        List<SetProjectMemberRoleRq> list = req.getMembers();

        for (SetProjectMemberRoleRq user: list) {

            UsersAccountEntity userToInvite = userRepo.findById(user.getUserId()).orElseThrow(
                    () -> new DataNotFoundException("User not found with ID: " + user.getUserId())
            );

            // Check if the user is already a member of the project
            if (projectMemberRepo.existsByProjectIdAndUserId(projId, user.getUserId())) {
                throw new DuplicatedDataException("The user is already existed in the project");
            }

            if (!StringUtils.equalsIgnoreCase(user.getUserRole(), UserRolesEnums.PROJECT_OWNER.getRole()) &&
                    !StringUtils.equalsIgnoreCase(user.getUserRole(), UserRolesEnums.PROJECT_USER.getRole())) {
                throw new InsertionFailException("Invalid user role");
            }

            ProjectMembersEntity newMember = new ProjectMembersEntity();

            newMember.setProject(proj);
            newMember.setUser(userToInvite);
            newMember.setProjectRole(user.getUserRole().toUpperCase());
            newMember.setInvitedBy(userRepo.getReferenceById(userId));
            newMember.setIsDeleted(false);

            projectMemberRepo.save(newMember);

        }

        proj.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("All users invited successfully.");
    }

    @Override
    @Transactional
    public RespMsg editProject(Integer projId, EditProjectReq req, Long userId) {

        // edit project name, description, and status

        ProjectsEntity project = projectRepo.findById(projId).orElseThrow(
                () -> new DataNotFoundException("Project not found"));

        if (!checkProjOwner(projId, userId)) {
            throw new InsertionFailException("Only project owner can edit project details");
        }

        // Cannot edit project when status is COMPLETED and user tries to change name or description
        if (StringUtils.equals(project.getStatus().getName(), ProjectTaskEnums.COMPLETED.getStatusName()) &&
                (StringUtils.isNotBlank(req.getDescription()) || StringUtils.isNotBlank(req.getProjectName()) ||
                        StringUtils.isNotBlank(req.getDeadline()))) {
            throw new InsertionFailException("Project is already completed, cannot edit");
        }

        // Update project details
        if (StringUtils.isNotBlank(req.getProjectName())) {
            project.setProjectName(req.getProjectName());
        }

        if (StringUtils.isNotBlank(req.getDescription())) {
            project.setDescription(req.getDescription());
        }
        if (StringUtils.isNotBlank(project.getDescription()) && StringUtils.isBlank(req.getDescription())) {
            project.setDescription(null);
        }

        if (StringUtils.isNotBlank(req.getStatusName())) {
            project.setStatus(statusRepo.findByName(req.getStatusName().toUpperCase())
                    .orElseThrow(() -> new DataNotFoundException("Status not found with name")));
        }

        if (StringUtils.isNotEmpty(req.getDeadline())) {
            project.setDeadline(parseDeadline(req.getDeadline()));
        }

        return createSuccessResponse("Project updated successfully.");
    }

    @Override
    @Transactional
    public RespMsg setProjectUserRole(Integer projId, SetProjectMemberRoleRq req, Long userId) {

        // ROLES string : OWNER, USER
        if (!StringUtils.equalsIgnoreCase(req.getUserRole(), UserRolesEnums.PROJECT_OWNER.getRole()) &&
                !StringUtils.equalsIgnoreCase(req.getUserRole(), UserRolesEnums.PROJECT_USER.getRole())) {
            throw new InsertionFailException("Cannot set user role");
        }

        // Logged-in user that is trying to set the role
        // Must be the project owner
        if (!checkProjOwner(projId, userId)) {
            throw new InsertionFailException("Only project owner can set user role in project");
        }

        // project exists and not COMPLETED
        ProjectsEntity project = projectRepo.findById(projId)
                .orElseThrow(() -> new DataNotFoundException("Project not found with ID"));

        // 要改ROLE的user
        // Exists in the project
        // is not the creator of the project
        ProjectMembersEntity memberToEdit = projectMemberRepo.findByUserIdAndProjectId(req.getUserId(), projId)
                .orElseThrow(() -> new DataNotFoundException("User is not a member of the project"));

        if (project.getCreator().getId().equals(memberToEdit.getUser().getId())) {
            throw new InsertionFailException("Cannot change project creator to other user role");
        }

        memberToEdit.setProjectRole(req.getUserRole().toUpperCase());
        project.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Project member role updated successfully.");
    }

    @Override
    @Transactional
    public RespMsg exitProject(Integer projId, Long removeUserId, Long actorUserId) {
        ProjectsEntity project = projectRepo.findById(projId)
                .orElseThrow(() -> new DataNotFoundException("Project not found with ID"));

        // creator cannot be removed from the project
        if (ObjectUtils.isNotEmpty(project.getCreator()) && removeUserId.equals(project.getCreator().getId())) {
            throw new DeleteFailException("Remove failed: project creator cannot be removed.");
        }

        ProjectMembersEntity target = projectMemberRepo.findByUserIdAndProjectId(removeUserId, projId)
                .orElseThrow(() -> new DataNotFoundException("User is not a member of the project"));

        // USER and OWNERs exit by themselves
        if (actorUserId.equals(removeUserId)) {
            projectMemberRepo.delete(target);
            project.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            return createSuccessResponse("Exited project successfully.");
        }

        if (checkProjOwner(projId, actorUserId)) {
            // owner can remove others except removing the creator
            projectMemberRepo.delete(target);
            project.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            return createSuccessResponse("User removed from project successfully.");

        }

        // user cannot remove others; owner cannot remove owner/creator
        throw new DeleteFailException("Remove user action failed.");
    }

    @Override
    @Transactional
    public RespMsg deleteProject(Integer id, Long userId) {
        ProjectsEntity proj = projectRepo.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Project not found with ID: " + id));

        if (ObjectUtils.isEmpty(proj.getCreator()) || !proj.getCreator().getId().equals(userId)) {
            throw new DeleteFailException("Project cannot be deleted");
        }

        projectRepo.delete(proj);

        return createSuccessResponse("Project deleted successfully.");
    }

    @Override
    public ProjectTagsListResp getProjectTags(Integer projId, Long userId) {
        if (!projectMemberRepo.existsByProjectIdAndUserId(projId, userId)
                && !StringUtils.equals(userRepo.getReferenceById(userId).getRole(), UserRolesEnums.ADMIN.getRole())) {
            throw new DataNotFoundException("Project not found");
        }

        List<TagsEntity> tags = tagRepo.findByProjectId(projId);
        ProjectTagsListResp resp = new ProjectTagsListResp();
        resp.setProjectId(projId);
        List<TagsResp> tagList = new ArrayList<>();

        for (TagsEntity tag : tags) {
            TagsResp tagResp = new TagsResp();
            tagResp.setTagId(tag.getId());
            tagResp.setTagName(tag.getName());
            tagResp.setCreatedAt(tag.getCreatedAt());
            tagResp.setUpdatedAt(tag.getUpdatedAt());
            tagResp.setCreator(ObjectUtils.isEmpty(tag.getCreator())?
                    UserRolesEnums.DELETED_USER.getRole():tag.getCreator().getUsername());

            tagList.add(tagResp);
        }

        resp.setTags(tagList);

        return resp;

    }

    @Override
    @Transactional
    public RespMsg addProjectTag(Integer id, ProjTagContentReq req, Long userId) {

        ProjectsEntity project = projectRepo.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Project not found with ID: " + id));

        if (!checkExistProjectEditable(id, userId)) {
            throw new InsertionFailException("Cannot add tag to this project");
        }

        if (tagRepo.existsByProjectIdAndName(id, req.getTagName())) {
            throw new DuplicatedDataException("Tag name already exists in this project");
        }

        TagsEntity newTag = new TagsEntity();
        newTag.setProject(project);
        newTag.setName(req.getTagName());
        newTag.setCreator(userRepo.getReferenceById(userId));
        newTag.setIsDeleted(false);

        tagRepo.save(newTag);
        project.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Tag added to project successfully.");

    }

    @Override
    @Transactional
    public RespMsg editProjectTag(Integer projId, Integer tagId, ProjTagContentReq req, Long userId) {

        TagsEntity tag = tagRepo.findByIdAndProjectId(tagId, projId)
                .orElseThrow(() -> new DataNotFoundException("Tag not found in this project"));

        if (!checkExistProjectEditable(projId, userId)) {
            throw new InsertionFailException("Cannot edit tag in this project");
        }


        if (tagRepo.existsByProjectIdAndName(projId, req.getTagName())) {
            throw new DuplicatedDataException("Tag name already exists in this project");
        }

        tag.setName(req.getTagName());
        tag.getProject().setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Tag updated successfully.");
    }

    @Override
    @Transactional
    public RespMsg deleteProjectTag(Integer projId, Integer tagId, Long userId) {
        TagsEntity tag = tagRepo.findByIdAndProjectId(tagId, projId)
                .orElseThrow(() -> new DataNotFoundException("Tag not found in this project"));

        if (!projectMemberRepo.existsByProjectIdAndUserId(projId, userId)) {
            throw new DeleteFailException("Tag not found.");
        }

        tagRepo.delete(tag);
        tag.getProject().setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Tag deleted successfully.");
    }


    /** Date time parsing */
    private Timestamp parseDeadline(String input) {

        try {
            LocalDate date = LocalDate.parse(input);
            return Timestamp.valueOf(date.atStartOfDay());
        } catch (DateTimeParseException e) {
            throw new InsertionFailException("Invalid date format.");
        }
    }

    /** Check if a project member can update project (status != COMPLETE, project exists and not deleted) */
    // user can still exit a completed project (不能邀請)
    private Boolean checkExistProjectEditable(Integer projectId, Long userId) {

        ProjectMembersEntity member = projectMemberRepo.findByUserIdAndProjectId(userId, projectId).orElseThrow(
                () -> new DataNotFoundException("Project member not found")
        );

        return !StringUtils.equals(member.getProject().getStatus().getName(),
                ProjectTaskEnums.COMPLETED.getStatusName());
    }

    /** Check if the user is the project owner */
    // 邀請 project user, edit project description, proj name, status, change project user role 需要
    private Boolean checkProjOwner (Integer projId, Long userId) {

        ProjectMembersEntity projectMember = projectMemberRepo.findByUserIdAndProjectId(userId, projId).orElseThrow(
                () -> new DataNotFoundException("Project member not found")
        );

        return projectMember.getProjectRole().equals(UserRolesEnums.PROJECT_OWNER.getRole());
    }

    /** Get all members list from a specific project. */
    private List<ProjectUserDetailsResp> fetchProjectMembers(Integer projectId) {
        List<ProjectMembersEntity> projectMembers = projectMemberRepo.findByProjectIdOrderByCreatedAtAsc(projectId);
        List<ProjectUserDetailsResp> projMemberDataList = new ArrayList<>();

        for (ProjectMembersEntity member : projectMembers) {

            ProjectUserDetailsResp memberData = new ProjectUserDetailsResp();

            if (ObjectUtils.isEmpty(member.getUser())) {
//                memberData.setName(UserRolesEnums.DELETED_USER.getRole());
                continue;
            } else {
                memberData.setId(member.getUser().getId());
                memberData.setName(member.getUser().getUsername());
                memberData.setEmail(member.getUser().getEmail());
                memberData.setRole(member.getProjectRole());
                memberData.setInvitedBy(ObjectUtils.isEmpty(member.getInvitedBy()) ?
                        UserRolesEnums.DELETED_USER.getRole() : member.getInvitedBy().getUsername());
                memberData.setCreatedAt(member.getCreatedAt());
                memberData.setUpdatedAt(member.getUpdatedAt());
            }

            projMemberDataList.add(memberData);
        }

        return projMemberDataList;
    }

    private RespMsg createSuccessResponse(String message) {

        RespMsg response = new RespMsg();

        response.setStatus(ResponseStatusEnums.SUCCESS.getStatus());
        response.setMessage(message);
        return response;
    }

}
