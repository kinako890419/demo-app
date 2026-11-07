package com.example.app.common.config;

import com.example.app.entity.OidcConfigEntity;
import com.example.app.entity.SamlConfigEntity;
import com.example.app.entity.StatusEntity;
import com.example.app.entity.UsersAccountEntity;
import com.example.app.common.enums.ProjectTaskEnums;
import com.example.app.common.enums.UserRolesEnums;
import com.example.app.repository.OidcConfigRepository;
import com.example.app.repository.SamlConfigRepository;
import com.example.app.repository.StatusRepository;
import com.example.app.repository.UsersAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Seeds local-dev data. Every step is idempotent: existing rows are never overwritten,
 * IdP rows only get their blank fields filled from the {@code seed.*} properties.
 */
@Slf4j
@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    @Autowired
    private UsersAccountRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private OidcConfigRepository oidcConfigRepository;

    @Autowired
    private SamlConfigRepository samlConfigRepository;

    @Value("${seed.oidc.keycloak.issuer-url:}")
    private String keycloakIssuerUrl;
    @Value("${seed.oidc.keycloak.client-id:}")
    private String keycloakClientId;
    @Value("${seed.oidc.keycloak.client-secret:}")
    private String keycloakClientSecret;

    @Value("${seed.oidc.google.client-id:}")
    private String googleClientId;
    @Value("${seed.oidc.google.client-secret:}")
    private String googleClientSecret;

    @Value("${seed.saml.saml-test.idp-sso-url:}")
    private String samlIdpSsoUrl;
    @Value("${seed.saml.saml-test.idp-entity-id:}")
    private String samlIdpEntityId;
    @Value("${seed.saml.saml-test.certificate:}")
    private String samlCertificate;

    @Override
    public void run(String[] args){
        if (!userRepository.existsByEmail("admin0@test.com")) {
            UsersAccountEntity admin = new UsersAccountEntity();
            admin.setUsername("admin");
            admin.setEmail("admin0@test.com");
            admin.setPassword(passwordEncoder.encode("123"));
            admin.setRole(UserRolesEnums.ADMIN.getRole());
            admin.setIsDeleted(false);
            userRepository.save(admin);
        }
        for (ProjectTaskEnums e : ProjectTaskEnums.values()) {
            if (statusRepository.findById(e.getStatusId()).isEmpty()
                    && statusRepository.findByName(e.getStatusName()).isEmpty()) {
                StatusEntity status = new StatusEntity();
                status.setId(e.getStatusId());
                status.setName(e.getStatusName());
                statusRepository.save(status);
            }
        }

        seedSso();
    }

    private void seedSso() {
        // SSO-only accounts (no password), matching the Keycloak users in dockerfiles/keycloak/app-realm.json
        createSsoUserIfNotExists("test@test.com", "test-keycloak-oidc"); // testuser, OIDC
        createSsoUserIfNotExists("test@test2.com", "test-saml");         // samluser, SAML

        seedOidcConfig("keycloak", "test\\.com", keycloakIssuerUrl, keycloakClientId, keycloakClientSecret);
        seedOidcConfig("google", "gmail\\.com", "https://accounts.google.com", googleClientId, googleClientSecret);
        seedSamlConfig("saml-test", "test2\\.com", samlIdpSsoUrl, samlIdpEntityId, samlCertificate);
    }

    private void createSsoUserIfNotExists(String email, String username) {
        if (!userRepository.existsByEmail(email)) {
            UsersAccountEntity user = new UsersAccountEntity();
            user.setUsername(username);
            user.setEmail(email);
            user.setRole(UserRolesEnums.USER.getRole());
            user.setIsDeleted(false);
            userRepository.save(user);
        }
    }

    private void seedOidcConfig(String idpName, String domainFilter,
                                String issuerUrl, String clientId, String clientSecret) {
        OidcConfigEntity existing = oidcConfigRepository.findByIdpName(idpName).orElse(null);

        if (existing == null && StringUtils.isBlank(clientId)) {
            log.info("Skip seeding OIDC IdP '{}': seed.oidc.{}.client-id is not set", idpName, idpName);
            return;
        }

        OidcConfigEntity oidcConfig = existing;
        if (oidcConfig == null) {
            oidcConfig = new OidcConfigEntity();
            oidcConfig.setIdpName(idpName);
            oidcConfig.setDomainFilter(domainFilter);
        }
        fillIfBlank(oidcConfig::getIssuerUrl, oidcConfig::setIssuerUrl, issuerUrl);
        fillIfBlank(oidcConfig::getClientId, oidcConfig::setClientId, clientId);
        fillIfBlank(oidcConfig::getClientSecret, oidcConfig::setClientSecret, clientSecret);

        oidcConfigRepository.save(oidcConfig);
    }

    private void seedSamlConfig(String idpName, String domainFilter,
                                String idpSsoUrl, String idpEntityId, String certificate) {
        SamlConfigEntity existing = samlConfigRepository.findByIdpName(idpName).orElse(null);

        if (existing == null && StringUtils.isBlank(certificate)) {
            log.info("Skip seeding SAML IdP '{}': seed.saml.{}.certificate is not set", idpName, idpName);
            return;
        }

        SamlConfigEntity samlConfig = existing;
        if (samlConfig == null) {
            samlConfig = new SamlConfigEntity();
            samlConfig.setIdpName(idpName);
            samlConfig.setDomainFilter(domainFilter);
        }
        fillIfBlank(samlConfig::getIdpSsoUrl, samlConfig::setIdpSsoUrl, idpSsoUrl);
        fillIfBlank(samlConfig::getIdpEntityId, samlConfig::setIdpEntityId, idpEntityId);
        fillIfBlank(samlConfig::getCertificate, samlConfig::setCertificate, certificate);

        samlConfigRepository.save(samlConfig);
    }

    private static void fillIfBlank(Supplier<String> getter, Consumer<String> setter, String value) {
        if (StringUtils.isBlank(getter.get()) && StringUtils.isNotBlank(value)) {
            setter.accept(value);
        }
    }
}
