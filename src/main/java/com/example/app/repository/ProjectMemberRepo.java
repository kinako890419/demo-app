package com.example.app.repository;

import com.example.app.entity.ProjectMembersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepo extends JpaRepository<ProjectMembersEntity, Integer> {

    Boolean existsByProjectIdAndUserId(Integer id, Long userId);

    Optional<ProjectMembersEntity> findByUserIdAndProjectId(Long userId, Integer id);

    List<ProjectMembersEntity> findByProjectIdOrderByCreatedAtAsc(Integer projectId);
}
