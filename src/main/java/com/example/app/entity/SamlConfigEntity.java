package com.example.app.entity;

import com.example.app.common.entity.BaseEntity;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashMap;
import java.util.Map;

/**
 * One SAML identity provider, ported from kinako890419/demo (dev).
 * Dev rows are seeded by {@link com.example.app.common.config.CommandLineAppStartupRunner}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "SAML_CONFIG")
@SQLDelete(sql = "UPDATE SAML_CONFIG SET IS_DELETED = true WHERE ID = ?")
@SQLRestriction("IS_DELETED = false")
public class SamlConfigEntity extends BaseEntity {

    /** Unique name of this IdP, also used in the ACS path. */
    @Column(unique = true, nullable = false)
    private String idpName;

    /** Regex matched against the whole email domain, e.g. {@code test2\.com}. */
    private String domainFilter;

    private String idpSsoUrl;
    private String idpEntityId;

    /** IdP signing certificate (Base64 body, no PEM header). */
    @Column(columnDefinition = "TEXT")
    private String certificate;

    @Transient
    public Saml2Settings toSaml2Settings(String baseUrl) {
        Map<String, Object> samlData = new HashMap<>();

        // sp
        samlData.put("onelogin.saml2.sp.entityid", "saml-test");
        samlData.put("onelogin.saml2.sp.assertion_consumer_service.url", baseUrl + getCallbackPath());

        // idp
        samlData.put("onelogin.saml2.idp.entityid", idpEntityId);
        samlData.put("onelogin.saml2.idp.single_sign_on_service.url", idpSsoUrl);
        samlData.put("onelogin.saml2.idp.single_sign_on_service.binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect");
        samlData.put("onelogin.saml2.idp.x509cert", certificate);

        SettingsBuilder builder = new SettingsBuilder();
        return builder.fromValues(samlData).build();
    }

    @Transient
    public String getCallbackPath() {
        return "/api/v1/saml/login/" + idpName;
    }
}
