package com.example.app.entity;

import com.example.app.common.entity.BaseEntity;
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
@Table(name = "SAML_CONFIG")
@SQLDelete(sql = "UPDATE SAML_CONFIG SET IS_DELETED = true WHERE ID = ?")
@SQLRestriction("IS_DELETED = false")
public class SamlConfigEntity extends BaseEntity {
    String idpName;
    String domainFilter;
}
