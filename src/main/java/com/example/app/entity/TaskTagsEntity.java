package com.example.app.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.*;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;

import static org.hibernate.annotations.NotFoundAction.IGNORE;

@Entity
@Data
@Table(name = "task_tags")
@SQLDelete(sql = "UPDATE task_tags SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TaskTagsEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    private TasksEntity task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", referencedColumnName = "id")
    @NotFound(action = IGNORE)
    private TagsEntity tag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", updatable = false)
    @NotFound(action = IGNORE)
    private UsersAccountEntity createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
