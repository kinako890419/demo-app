package com.example.app.service;

import com.example.app.model.*;
import com.example.app.common.models.RespMsg;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TaskService {

    ViewAllTaskResp viewTasksByProjId (Integer projId, String tagIdListParam, String status,
                                       String sortBy, String order, Long userId);

    TaskDetailResp viewTaskDetails(Integer projectId, Integer taskId, Long id);

    /** New task with task name, creator, status, description and deadline */
    TaskResp addTask (Integer projectId, AddNewTaskReq req, Long userId);

    RespMsg updateTask(Integer projectId, Integer taskId, EditTaskReq req, Long id);

    RespMsg deleteTask(Integer projectId, Integer taskId, Long id);

    RespMsg assignTaskMember(Integer projectId, Integer taskId, List<AssignTaskMemberReq> req, Long id);

    RespMsg removeUserFromTask(Integer projectId, Integer taskId, Long userId, Long id);

    RespMsg addCommentToTask(Integer projectId, Integer taskId, CommentContentReq req, Long id);

    RespMsg updateCommentInTask(Integer projectId, Integer taskId, Integer commentId, CommentContentReq req, Long id);

    RespMsg deleteCommentFromTask(Integer projectId, Integer taskId, Integer commentId, Long id);

    RespMsg addTagToTask(Integer projectId, Integer taskId, AddTaskTagsReq req, Long id);

    RespMsg deleteTagFromTask(Integer projectId, Integer taskId, Integer tagId, Long id);
}
