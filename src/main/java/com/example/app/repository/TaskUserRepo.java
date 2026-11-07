package com.example.app.repository;

import com.example.app.entity.TaskUsersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskUserRepo extends JpaRepository<TaskUsersEntity, Integer> {

    Boolean existsByUserIdAndTaskId(Long userId, Integer taskId);

    List<TaskUsersEntity> findByTaskId(Integer id);

    Optional<TaskUsersEntity> findByUserIdAndTaskId(Long userId, Integer taskId);

}
