package com.example.app.repository;

import com.example.app.entity.UsersAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsersAccountRepository extends JpaRepository<UsersAccountEntity, Long> {
    boolean existsByEmail(String email);
    Optional<UsersAccountEntity> findByEmail(String email);

    /** Native query bypasses the @SQLRestriction soft-delete filter. */
    @Query(value = "SELECT * FROM users_account WHERE is_deleted = true", nativeQuery = true)
    List<UsersAccountEntity> findDeletedUsers();

    List<UsersAccountEntity> findAllByOrderByCreatedTimeAsc();
}
