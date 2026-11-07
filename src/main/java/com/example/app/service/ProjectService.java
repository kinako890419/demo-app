package com.example.app.service;

import com.example.app.model.*;
import com.example.app.common.models.RespMsg;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProjectService {

    /** View personal projects or ADMIN view all projects */
    List<ProjDetailsResp> viewAllProjects(String sortBy, String order,
                                          Integer page, Integer pageSize,
                                          String statusName, Long userId, String userRole);

    ProjDetailsResp getProjByProjectId(Integer projectId, Long userId, String userRole);

    /** Create a new project (pending) */
    RespMsg createProject(CreateProjectReq req, Long userId);

    /** invite user (existed proj) and set role in the project */
    RespMsg assignProjectMember(Integer projId, SetProjectMemberRoleRqList req, Long userId);

    RespMsg editProject(Integer projId, EditProjectReq req, Long userId);

    RespMsg exitProject(Integer projId, Long userId, Long id);

    RespMsg setProjectUserRole(Integer projId, SetProjectMemberRoleRq req, Long id);

    RespMsg deleteProject(Integer projId, Long userId);

    ProjectTagsListResp getProjectTags(Integer projId, Long userId);

    RespMsg addProjectTag(Integer id, ProjTagContentReq req, Long userId);

    RespMsg editProjectTag(Integer id, Integer tagId, ProjTagContentReq req, Long userId);

    RespMsg deleteProjectTag(Integer id, Integer tagId, Long userId);

}
