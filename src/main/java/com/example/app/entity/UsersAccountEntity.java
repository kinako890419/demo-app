package com.example.app.entity;

import com.example.app.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "USERS_ACCOUNT")
@SQLDelete(sql = "UPDATE users_account SET is_deleted = true WHERE id = ?")
@SQLRestriction("IS_DELETED = false")
public class UsersAccountEntity extends BaseEntity {

    @Column(name = "user_name")
    private String username;

    @Column(name="email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    /** ADMIN or USER, see {@link com.example.app.common.enums.UserRolesEnums} */
    @Column(name = "role", nullable = false)
    private String role = "USER";

}
