package com.example.app.repository;

import com.example.app.entity.TagsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagsRepository extends JpaRepository<TagsEntity, Integer> {

    Boolean existsByProjectIdAndName(Integer id, String tagName);

    List<TagsEntity> findByProjectId(Integer projId);

    Optional<TagsEntity> findByIdAndProjectId(Integer tagId, Integer projId);
}
