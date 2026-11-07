package com.example.app.repository;

import com.example.app.entity.TasksEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<TasksEntity, Integer> {

    List<TasksEntity> findByProjectId(Integer id, Sort sort);

    List<TasksEntity> findByProjectIdAndStatusId(Integer projId, Integer statusId, Sort sort);

    Boolean existsByProjectIdAndId(Integer projId, Integer taskId);

    Optional<TasksEntity> findByProjectIdAndId(Integer projId, Integer taskId);
}
