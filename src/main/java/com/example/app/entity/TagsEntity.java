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
@Table(name = "tags")
@Data
@SQLDelete(sql = "UPDATE tags SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class TagsEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private ProjectsEntity project;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", referencedColumnName = "id", updatable = false)
    @NotFound(action = IGNORE)
    private UsersAccountEntity creator;

    @CreationTimestamp
    @Column(name = "created_at")
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
