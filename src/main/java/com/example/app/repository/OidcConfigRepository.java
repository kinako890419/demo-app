package com.example.app.repository;

import com.example.app.entity.OidcConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OidcConfigRepository extends JpaRepository<OidcConfigEntity, Long> {
    Optional<OidcConfigEntity> findByIdpName(String idpName);
}
