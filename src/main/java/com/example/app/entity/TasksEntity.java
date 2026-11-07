package com.example.app.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.annotations.*;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

import static org.hibernate.annotations.NotFoundAction.IGNORE;

@Entity
@Data
@Table(name = "tasks")
@SQLDelete(sql = "UPDATE tasks SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TasksEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private ProjectsEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", referencedColumnName = "id", updatable = false)
    @NotFound(action = IGNORE)
    private UsersAccountEntity creator;

    @Column(name = "task_name")
    @NotBlank(message = "Task name cannot be blank")
    private String taskName;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", referencedColumnName = "id")
    private StatusEntity status;

    @CreationTimestamp
    @Column(name = "created_at")
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "deadline")
    private Timestamp deadline;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
