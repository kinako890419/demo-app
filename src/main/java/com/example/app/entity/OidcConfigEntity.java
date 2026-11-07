package com.example.app.entity;

import com.example.app.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * One OIDC identity provider. Rows are read at runtime, so IdPs can be added without a redeploy.
 * Dev rows are seeded by {@link com.example.app.common.config.CommandLineAppStartupRunner}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "OIDC_CONFIG")
@SQLDelete(sql = "UPDATE OIDC_CONFIG SET IS_DELETED = true WHERE ID = ?")
@SQLRestriction("IS_DELETED = false")
public class OidcConfigEntity extends BaseEntity {

    /** Unique name of this IdP, also used in the callback path. */
    @Column(unique = true, nullable = false)
    private String idpName;

    /** Regex matched against the whole email domain, e.g. {@code test\.com}. */
    private String domainFilter;

    private String issuerUrl;
    private String clientId;

    @Column(columnDefinition = "TEXT")
    private String clientSecret;

    @Transient
    public String getCallbackPath() {
        return "/api/v1/oidc/" + idpName + "/callback";
    }

    @Transient
    public String getOidcScope() {
        return "openid email";
    }
}
