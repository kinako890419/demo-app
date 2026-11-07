package com.example.app.service;

import com.example.app.common.exception.DataNotFoundException;
import com.example.app.common.models.UrlResponse;
import com.example.app.common.util.EmailUtils;
import com.example.app.entity.OidcConfigEntity;
import com.example.app.entity.SamlConfigEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Picks SAML or OIDC by the email domain. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoService {

    private final SamlAuthService samlAuthService;
    private final OidcAuthService oidcAuthService;

    public UrlResponse ssoLogin(String email, boolean isLocalDev) {
        String domain = EmailUtils.getDomain(email);

        // SAML takes precedence when both protocols match the domain
        Optional<SamlConfigEntity> samlConfig = samlAuthService.getSamlConfigByDomain(domain);
        if (samlConfig.isPresent()) {
            log.info("Using SAML IdP '{}' for SSO login", samlConfig.get().getIdpName());
            return samlAuthService.getSamlIdpUrl(samlConfig.get(), isLocalDev);
        }

        OidcConfigEntity oidcConfig = oidcAuthService.getOidcConfigByDomain(domain).orElseThrow(
                () -> new DataNotFoundException("No SSO configuration found for domain: " + domain));
        log.info("Using OIDC IdP '{}' for SSO login", oidcConfig.getIdpName());
        return oidcAuthService.getOidcIdpRedirectUrl(oidcConfig, email, isLocalDev);
    }
}
