package com.example.app.service.impl;

import com.example.app.model.*;
import com.example.app.entity.*;
import com.example.app.repository.*;
import com.example.app.common.models.RespMsg;
import com.example.app.common.enums.ProjectTaskEnums;
import com.example.app.common.enums.ResponseStatusEnums;
import com.example.app.common.enums.UserRolesEnums;
import com.example.app.common.exception.DataNotFoundException;
import com.example.app.common.exception.DeleteFailException;
import com.example.app.common.exception.DuplicatedDataException;
import com.example.app.common.exception.InsertionFailException;
import com.example.app.service.TaskService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private ProjectRepository projectRepo;

    @Autowired
    private ProjectMemberRepo projectMemberRepo;

    @Autowired
    private TaskRepository taskRepo;

    @Autowired
    private TaskUserRepo taskUserRepo;

    @Autowired
    private TaskAttachmentsRepo taskAttachmentRepo;

    @Autowired
    private TaskCommentsRepo taskCommentsRepo;

    @Autowired
    private TagsRepository tagsRepo;

    @Autowired
    private TaskTagsRepo taskTagsRepo;

    @Autowired
    private StatusRepository statusRepo;

    @Autowired
    private UsersAccountRepository userRepo;


    @Override
    public ViewAllTaskResp viewTasksByProjId(Integer projId, String tagIdListParam, String status,
                                             String sortBy, String order, Long userId) {

        // All members can view tasks in the project
        if (!StringUtils.equals(userRepo.getReferenceById(userId).getRole(), UserRolesEnums.ADMIN.getRole())
                && !projectMemberRepo.existsByProjectIdAndUserId(projId, userId)) {
            throw new DataNotFoundException("Cannot view tasks in this project");
        }

        Set<String> allowedFields = Set.of("createdAt", "updatedAt", "taskName", "deadline");
        String validSortBy = allowedFields.contains(sortBy) ? sortBy : "createdAt";

        // Default sort tasks by created time
        Sort sort = order.equalsIgnoreCase("desc") ?
                Sort.by(validSortBy).descending() :
                Sort.by(validSortBy).ascending();

        List<TasksEntity> allTasks = Optional.ofNullable(status)
                .filter(StringUtils::isNotBlank)
                .map(statusName -> statusRepo.findByName(statusName).orElseThrow(  // 有 status
                        () -> new DataNotFoundException("Status not found with name: " + statusName)))
                .map(statusEntity ->
                        taskRepo.findByProjectIdAndStatusId(projId, statusEntity.getId(), sort))
                .orElseGet(() -> // 無 status
                        taskRepo.findByProjectId(projId, sort));

        List<TaskListResp> tasksList = new ArrayList<>();

        for (TasksEntity task: allTasks) {

            TaskListResp taskListItem = new TaskListResp();

            taskListItem.setTaskId(task.getId());
            taskListItem.setTaskName(task.getTaskName());
            taskListItem.setDescription(task.getDescription());
            taskListItem.setStatus(task.getStatus().getName());
            taskListItem.setCreator(ObjectUtils.isEmpty(task.getCreator()) ?
                    UserRolesEnums.DELETED_USER.getRole() : task.getCreator().getUsername());
            taskListItem.setDeadline(task.getDeadline());
            taskListItem.setCreatedAt(task.getCreatedAt());
            taskListItem.setUpdatedAt(task.getUpdatedAt());
            taskListItem.setCanEdit(checkTaskCanEdit(projId, task.getId(), userId));

            // Get task users
            List<TaskUserDetailsResp> userProfiles = fetchAllTaskUsers(task.getId());
            taskListItem.setTaskMembers(userProfiles);

            tasksList.add(taskListItem);
        }

        ViewAllTaskResp resp = new ViewAllTaskResp();
        resp.setProjectId(projId);
        resp.setTaskList(tasksList);

        return resp;

    }

    @Override
    public TaskDetailResp viewTaskDetails(Integer projId, Integer taskId, Long userId) {

        TasksEntity task = taskRepo.findByProjectIdAndId(projId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Task not found"));

        // Project exists, tasks in the project exists
        // Project user (member) can view the task and its details
        if (!projectMemberRepo.existsByProjectIdAndUserId(projId, userId)
                && !StringUtils.equals(userRepo.getReferenceById(userId).getRole(), UserRolesEnums.ADMIN.getRole())) {
            throw new DataNotFoundException("Task not found or you are not a member");
        }

        TaskDetailResp taskDetailResp = new TaskDetailResp();

        taskDetailResp.setTaskId(task.getId());
        taskDetailResp.setTaskName(task.getTaskName());
        taskDetailResp.setDescription(task.getDescription());
        taskDetailResp.setCreatorName(ObjectUtils.isEmpty(task.getCreator()) ?
                UserRolesEnums.DELETED_USER.getRole() : task.getCreator().getUsername());
        taskDetailResp.setStatus(task.getStatus().getName());
        taskDetailResp.setCreatedAt(task.getCreatedAt());
        taskDetailResp.setDeadline(task.getDeadline());
        taskDetailResp.setUpdatedAt(task.getUpdatedAt());
        taskDetailResp.setCanEdit(checkTaskCanEdit(projId, taskId, userId));

        // Get task users
        List<TaskUserDetailsResp> userProfiles = fetchAllTaskUsers(taskId);
        taskDetailResp.setMemberLists(userProfiles);

        // Get task attachments
        List<TaskAttachmentsEntity> files = taskAttachmentRepo.findByTaskId(taskId);
        List<TaskAttachmentResp> attachments = new ArrayList<>();

        for (TaskAttachmentsEntity attach: files) {

            TaskAttachmentResp attachResp = new TaskAttachmentResp();
            attachResp.setId(attach.getId());
            attachResp.setFileName(attach.getOrigFileName());
            attachResp.setFileSize(attach.getFileSize());
            attachResp.setContentType(attach.getContentType());
            attachResp.setCreatorName(ObjectUtils.isEmpty(attach.getCreator())?
                    UserRolesEnums.DELETED_USER.getRole() : attach.getCreator().getUsername());
            attachResp.setCreatedAt(attach.getCreatedAt());
            attachResp.setUpdatedAt(attach.getUpdatedAt());

            attachments.add(attachResp);
        }
        taskDetailResp.setTaskAttachments(attachments);

        // Get task comments
        List<TaskCommentsEntity> comments = taskCommentsRepo.findByTaskIdOrderByIdAsc(taskId);
        List<TaskCommentResp> commentList = new ArrayList<>();

        for (TaskCommentsEntity comment: comments) {

            TaskCommentResp commentResp = new TaskCommentResp();
            commentResp.setCommentId(comment.getId());
            commentResp.setContent(comment.getContent());
            commentResp.setUserName(ObjectUtils.isEmpty(comment.getCreator()) ?
                    UserRolesEnums.DELETED_USER.getRole() : comment.getCreator().getUsername());
            commentResp.setCreatedAt(comment.getCreatedAt());
            commentResp.setUpdatedAt(comment.getUpdatedAt());

            commentList.add(commentResp);
        }
        taskDetailResp.setTaskComments(commentList);

        List<TaskTagsEntity> tagsList = taskTagsRepo.findByTaskId(taskId);
        List<TagsResp> tags = new ArrayList<>();

        for (TaskTagsEntity tag : tagsList) {
            TagsResp tagResp = new TagsResp();
            if (ObjectUtils.isEmpty(tag.getTag())) { // get project tag
                continue;
            }
            tagResp.setTagId(tag.getTag().getId());
            tagResp.setTagName(tag.getTag().getName());
            tagResp.setCreator(ObjectUtils.isEmpty(tag.getCreatedBy()) ?
                    UserRolesEnums.DELETED_USER.getRole() : tag.getCreatedBy().getUsername());
            tagResp.setCreatedAt(tag.getCreatedAt());
            tagResp.setUpdatedAt(tag.getUpdatedAt());
            tags.add(tagResp);
        }

        taskDetailResp.setTags(tags);

        return taskDetailResp;

    }

    @Override
    @Transactional
    public TaskResp addTask(Integer projId, AddNewTaskReq req, Long userId) {

        ProjectMembersEntity projectMember = projectMemberRepo
                .findByUserIdAndProjectId(userId, projId)
                .orElseThrow(() -> new DataNotFoundException("You are not a member of this project"));

        // not project member or project not exist or project completed
        if (StringUtils.equals(projectMember.getProject().getStatus().getName(),
                        ProjectTaskEnums.COMPLETED.getStatusName())) {
            throw new InsertionFailException("Cannot create task in this project");
        }

        UsersAccountEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found"));

        TasksEntity task = new TasksEntity();

        task.setProject(projectMember.getProject());
        task.setCreator(projectMember.getUser());
        task.setTaskName(req.getTaskName());
        task.setDescription(req.getDescription());

        if (StringUtils.isEmpty(req.getStatus())) {
            task.setStatus(statusRepo.findByName(ProjectTaskEnums.PENDING.getStatusName()).orElse(null));
        } else {
            task.setStatus(statusRepo.findByName(req.getStatus().toUpperCase()).orElseThrow(
                    () -> new InsertionFailException("Status not found with name: " + req.getStatus())
            ));
        }

        checkProjDeadline(projId, parseDeadline(req.getDeadline()));

        task.setDeadline(parseDeadline(req.getDeadline()));
        task.setIsDeleted(false); // default

        taskRepo.save(task);

        // Add task member (task user)

        TaskUsersEntity addCreator = new TaskUsersEntity();

        addCreator.setUser(user);
        addCreator.setTask(task);
        addCreator.setInvitedBy(user);
        addCreator.setIsDeleted(false);

        taskUserRepo.save(addCreator);
        task.getProject().setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        TaskResp resp = new TaskResp();
        resp.setTaskId(task.getId());
        resp.setTaskName(task.getTaskName());
        resp.setStatus("RUNNING");

        return resp;
    }

    @Override
    @Transactional
    public RespMsg updateTask(Integer projectId, Integer taskId, EditTaskReq req, Long id) {

        TasksEntity task = taskRepo.findByProjectIdAndId(projectId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Task not found"));

        if (!checkTaskCanEdit(projectId, taskId, id)) {
            throw new InsertionFailException("You cannot edit this task");
        }

        if (checkProjTaskCompleted(task) && (StringUtils.isNotEmpty(req.getTaskName())
                || StringUtils.isNotEmpty(req.getDescription())
                || StringUtils.isNotEmpty(req.getDeadline()))) {
            throw new InsertionFailException("Project or task is completed, cannot edit task details");
        }

        if (StringUtils.isNotEmpty(req.getTaskName())) {
            task.setTaskName(req.getTaskName());
        }

        if (StringUtils.isNotEmpty(req.getDescription())) {
            task.setDescription(req.getDescription());
        }
        if (StringUtils.isNotBlank(task.getDescription()) && StringUtils.isBlank(req.getDescription())) {
            task.setDescription(null);
        }

        if (StringUtils.isNotEmpty(req.getDeadline())) {
            checkProjDeadline(projectId, parseDeadline(req.getDeadline()));
            task.setDeadline(parseDeadline(req.getDeadline()));
        }
        if (StringUtils.isNotEmpty(req.getStatus())) {
            task.setStatus(statusRepo.findByName(req.getStatus().toUpperCase())
                    .orElseThrow(() -> new DataNotFoundException("Status not found with name")));
        }

        taskRepo.save(task);
        task.getProject().setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Task updated successfully");

    }

    @Override
    @Transactional
    public RespMsg deleteTask(Integer projectId, Integer taskId, Long id) {
        TasksEntity task = taskRepo.findByProjectIdAndId(projectId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Task not found"));

        if (!checkTaskCanEdit(projectId, taskId, id)) {
            throw new DeleteFailException("You cannot delete this task");
        }

        taskRepo.delete(task);
        projectRepo.getReferenceById(projectId).setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Task deleted successfully");
    }

    @Override
    @Transactional
    public RespMsg assignTaskMember(Integer projId, Integer taskId, List<AssignTaskMemberReq> req, Long id) {

        TasksEntity task = taskRepo.findByProjectIdAndId(projId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Task not found"));

        if (!checkTaskCanEdit(projId, taskId, id) || checkProjTaskCompleted(task)) {
            throw new InsertionFailException("You cannot assign member to this task");
        }

        for (AssignTaskMemberReq userListMember: req){

            if (!projectMemberRepo.existsByProjectIdAndUserId(projId, userListMember.getUserId())) {
                throw new DataNotFoundException("User not found with id: " + userListMember.getUserId());
            }

            if (taskUserRepo.existsByUserIdAndTaskId(userListMember.getUserId(), taskId)) {
                throw new DuplicatedDataException("User is already assigned to this task");
            }

            TaskUsersEntity taskUser = new TaskUsersEntity();
            taskUser.setUser(userRepo.getReferenceById(userListMember.getUserId()));
            taskUser.setTask(task);
            taskUser.setIsDeleted(false);
            taskUser.setInvitedBy(userRepo.getReferenceById(id));

            taskUserRepo.save(taskUser);
        }

        task.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Member assigned to task successfully");
    }

    @Override
    @Transactional
    public RespMsg removeUserFromTask(Integer projectId, Integer taskId, Long userId, Long id) {

        TasksEntity task = taskRepo.findByProjectIdAndId(projectId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Task not found"));

        if (!checkTaskCanEdit(projectId, taskId, id)) {
            throw new DataNotFoundException("You cannot remove member from this task");
        }

        TaskUsersEntity taskUser = taskUserRepo.findByUserIdAndTaskId(userId, taskId).orElseThrow(
                () -> new DataNotFoundException("User not found in this task")
        );

        if (!ObjectUtils.isEmpty(taskUser.getTask().getCreator())
                && taskUser.getTask().getCreator().getId().equals(userId)) {
            throw new DeleteFailException("You cannot remove the task creator from the task");
        }

        taskUserRepo.delete(taskUser);
        task.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Member removed from task successfully");

    }

    @Override
    @Transactional
    public RespMsg addCommentToTask(Integer projectId, Integer taskId, CommentContentReq req, Long userId) {

        TasksEntity task = taskRepo.findByProjectIdAndId(projectId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Task not found"));

        if (!checkTaskCanEdit(projectId, taskId, userId) ||
                checkProjTaskCompleted(task)) {
            throw new DataNotFoundException("Cannot add comment to this task");
        }

        TaskCommentsEntity comment = new TaskCommentsEntity();
        comment.setTask(task);
        comment.setCreator(userRepo.getReferenceById(userId));
        comment.setContent(req.getContent());
        comment.setIsDeleted(false);

        taskCommentsRepo.save(comment);
        task.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Comment added successfully");

    }

    @Override
    @Transactional
    public RespMsg updateCommentInTask(Integer projectId, Integer taskId, Integer commentId,
                                           CommentContentReq req, Long userId) {

        TasksEntity task = taskRepo.findByProjectIdAndId(projectId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Task not found"));

        TaskCommentsEntity comment = taskCommentsRepo.findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Comment not found"));

        // comment creator can edit the comment
        if (!checkTaskCanEdit(projectId, taskId, userId)
                || ObjectUtils.isEmpty(comment.getCreator())
                || !comment.getCreator().getId().equals(userId)
                || checkProjTaskCompleted(task)) {
            throw new InsertionFailException("You cannot edit this comment");
        }

        comment.setContent(req.getContent());

        taskCommentsRepo.save(comment);
        task.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Comment updated successfully");

    }

    @Override
    @Transactional
    public RespMsg deleteCommentFromTask(Integer projectId, Integer taskId, Integer commentId, Long userId) {

        TaskCommentsEntity comment = taskCommentsRepo.findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Comment not found"));

        if (!checkTaskCanEdit(projectId, taskId, userId)) {
            throw new DataNotFoundException("You cannot delete this comment");
        }

        // comment creator (task user) can delete the comment
        taskCommentsRepo.delete(comment);
        taskRepo.getReferenceById(taskId).setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Comment deleted successfully");

    }

    @Override
    @Transactional
    public RespMsg addTagToTask(Integer projectId, Integer taskId, AddTaskTagsReq req, Long id) {

        TasksEntity task = taskRepo.findByProjectIdAndId(projectId, taskId)
                .orElseThrow(() -> new DataNotFoundException("Task not found"));

        if (!checkTaskCanEdit(projectId, taskId, id) || checkProjTaskCompleted(task)) {
            throw new InsertionFailException("You cannot add tag to this task");
        }

        if (taskTagsRepo.existsByTagIdAndTaskId(req.getTagId(), taskId)) {
            throw new DuplicatedDataException("Tag already exists in this task");
        }

        TaskTagsEntity tag = new TaskTagsEntity();
        tag.setTask(taskRepo.getReferenceById(taskId));
        tag.setTag(tagsRepo.getReferenceById(req.getTagId()));
        tag.setCreatedBy(userRepo.getReferenceById(id));
        tag.setIsDeleted(false);
        taskRepo.getReferenceById(taskId).setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        taskTagsRepo.save(tag);

        return createSuccessResponse("Tag added to task successfully");
    }

    @Override
    public RespMsg deleteTagFromTask(Integer projectId, Integer taskId, Integer tagId, Long id) {

        if (!checkTaskCanEdit(projectId, taskId, id)) {
            throw new DeleteFailException("You cannot delete tag from this task");
        }

        TaskTagsEntity tag = taskTagsRepo.findByTagIdAndTaskId(tagId, taskId).orElseThrow(
                () -> new DataNotFoundException("Tag not found in this task")
        );

        taskTagsRepo.delete(tag);
        tag.getTask().setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return createSuccessResponse("Tag deleted from task successfully");
    }


    private void checkProjDeadline(Integer projId, Timestamp taskDeadline) {
        if (projectRepo.getReferenceById(projId).getDeadline().before(taskDeadline)) {
            throw new InsertionFailException("Task deadline cannot be later than project deadline");
        }
    }

    /** Check if user can edit the task in a project */
    private Boolean checkTaskCanEdit(Integer projectId, Integer taskId, Long userId) {

        // project owner = true
        ProjectMembersEntity projectMember = projectMemberRepo.
                findByUserIdAndProjectId(userId, projectId).orElse(null);

        if (ObjectUtils.isEmpty(projectMember) || !taskRepo.existsByProjectIdAndId(projectId, taskId)) {
            return false;
        }

        if (StringUtils.equals(projectMember.getProjectRole(), UserRolesEnums.PROJECT_OWNER.getRole())) {
            return true;
        }

        // task owner (task user) = true
        return taskUserRepo.existsByUserIdAndTaskId(userId, taskId);

    }

    private Boolean checkProjTaskCompleted(TasksEntity task) {

        return StringUtils.equals(task.getProject().getStatus().getName(), ProjectTaskEnums.COMPLETED.getStatusName()) ||
                StringUtils.equals(task.getStatus().getName(), ProjectTaskEnums.COMPLETED.getStatusName());

    }

    /** Date time parsing */
    private Timestamp parseDeadline(String input) {

        // DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // LocalDateTime dateTime = LocalDateTime.parse(input, dateTimeFormatter);

        try {
            LocalDate date = LocalDate.parse(input);
            return Timestamp.valueOf(date.atStartOfDay());
        } catch (DateTimeParseException e) {
            throw new InsertionFailException("Invalid date format.");
        }
        // return Timestamp.valueOf(dateTime);
    }

    private List<TaskUserDetailsResp> fetchAllTaskUsers(Integer taskId) {
        List<TaskUsersEntity> taskUsers = taskUserRepo.findByTaskId(taskId);
        List<TaskUserDetailsResp> userProfiles = new ArrayList<>();

        for (TaskUsersEntity taskUser : taskUsers) {

            TaskUserDetailsResp userResp = new TaskUserDetailsResp();

            if (ObjectUtils.isEmpty(taskUser.getUser())) {
//                userResp.setName(UserRolesEnums.DELETED_USER.getRole());
                continue;
            } else {
                userResp.setId(taskUser.getUser().getId());
                userResp.setName(taskUser.getUser().getUsername());
                userResp.setEmail(taskUser.getUser().getEmail());
                userResp.setInvitedBy(ObjectUtils.isEmpty(taskUser.getInvitedBy()) ?
                        UserRolesEnums.DELETED_USER.getRole() : taskUser.getInvitedBy().getUsername());
                userResp.setCreatedAt(taskUser.getCreatedAt());
                userResp.setUpdatedAt(taskUser.getUpdatedAt());
            }

            userProfiles.add(userResp);
        }

        return userProfiles;
    }

    private RespMsg createSuccessResponse(String message) {
        RespMsg resp = new RespMsg();
        resp.setStatus(ResponseStatusEnums.SUCCESS.getStatus());
        resp.setMessage(message);
        return resp;
    }

}
