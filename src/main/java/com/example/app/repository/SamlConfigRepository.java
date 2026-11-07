package com.example.app.repository;

import com.example.app.entity.SamlConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SamlConfigRepository extends JpaRepository<SamlConfigEntity, Long> {
    Optional<SamlConfigEntity> findByIdpName(String idpName);
}
