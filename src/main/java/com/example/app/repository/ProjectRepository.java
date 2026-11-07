package com.example.app.repository;

import com.example.app.entity.ProjectsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ProjectRepository extends JpaRepository<ProjectsEntity, Integer> {

    /** query all projects that the user is a member of,
     * if statusId is not null, filter by statusId */
    @Query("""
    SELECT DISTINCT p
    FROM ProjectsEntity p
    JOIN ProjectMembersEntity pm ON pm.project = p
    WHERE pm.user.id = :userId
    AND (:statusId IS NULL OR p.status.id = :statusId)
    """)
    Page<ProjectsEntity> findProjectsForUser(@Param("userId") Long userId,
                                             @Param("statusId") Integer statusId,
                                             Pageable pageable);

    /** no check if the account is the member in a project */
    @Query("""
    SELECT p
    FROM ProjectsEntity p
    WHERE (:statusId IS NULL OR p.status.id = :statusId)
    """) // 如果 isDeleted=null then return false
    Page<ProjectsEntity> findAllProjectsForAdmin(@Param("statusId") Integer statusId, Pageable pageable);

}
