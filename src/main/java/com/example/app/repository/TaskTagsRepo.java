package com.example.app.repository;

import com.example.app.entity.TaskTagsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskTagsRepo extends JpaRepository<TaskTagsEntity, Integer> {
    List<TaskTagsEntity> findByTaskId(Integer taskId);

    Boolean existsByTagIdAndTaskId(Integer tagId, Integer taskId);

    Optional<TaskTagsEntity> findByTagIdAndTaskId(Integer tagId, Integer taskId);
}
