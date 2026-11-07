package com.example.app.service.impl;

import com.example.app.common.config.AppProperties;
import com.example.app.common.exception.DataNotFoundException;
import com.example.app.common.exception.SamlAuthFailedException;
import com.example.app.common.models.UrlResponse;
import com.example.app.common.util.EmailUtils;
import com.example.app.entity.SamlConfigEntity;
import com.example.app.entity.UsersAccountEntity;
import com.example.app.model.LoginResp;
import com.example.app.model.UserProfileResp;
import com.example.app.repository.SamlConfigRepository;
import com.example.app.repository.UsersAccountRepository;
import com.example.app.security.CustomUserDetails;
import com.example.app.security.jwt.JwtService;
import com.example.app.security.saml.CustomSamlMessageFactory;
import com.example.app.service.SamlAuthService;
import com.onelogin.saml2.Auth;
import com.onelogin.saml2.authn.AuthnRequestParams;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.settings.Saml2Settings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * SP-initiated SAML login with onelogin java-saml, ported from kinako890419/demo (dev) as is:
 * {@link com.example.app.security.saml.CustomSamlResponse} skips the Destination, Recipient and Audience
 * checks, and no AuthnRequest ID is stored, so InResponseTo is not checked.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SamlAuthServiceImpl implements SamlAuthService {

    private final AppProperties appProperties;
    private final UsersAccountRepository accountRepository;
    private final SamlConfigRepository samlConfigRepository;
    private final JwtService jwtService;

    @Override
    public UrlResponse getIdpUrlByEmail(String email, boolean isLocalDev) {
        String domain = EmailUtils.getDomain(email);
        SamlConfigEntity samlConfig = getSamlConfigByDomain(domain)
                .orElseThrow(() -> new DataNotFoundException("No SAML IdP configured for email domain: " + domain));

        return getSamlIdpUrl(samlConfig, isLocalDev);
    }

    @Override
    public UrlResponse getSamlIdpUrl(SamlConfigEntity samlConfig, boolean isLocalDev) {
        String baseUrl = appProperties.getBaseUrl(isLocalDev);
        Saml2Settings settings = samlConfig.toSaml2Settings(baseUrl);

        try {
            Auth auth = getAuth(settings, null, null);
            AuthnRequestParams authnRequestParams = new AuthnRequestParams(false, false, false);
            String targetUrl = auth.login(baseUrl, authnRequestParams, true);

            log.info("SAML AuthnRequest URL: {}", targetUrl);
            log.debug("Last AuthnRequest XML: {}", auth.getLastRequestXML());

            return UrlResponse.builder().url(targetUrl).build();
        } catch (Exception ex) {
            log.error("Get SAML IdP url failed", ex);
            throw new IllegalStateException("Get SAML IdP url failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<SamlConfigEntity> getSamlConfigByDomain(String domain) {
        for (SamlConfigEntity samlConfig : samlConfigRepository.findAll()) {
            if (samlConfig.getDomainFilter() != null && domain.matches(samlConfig.getDomainFilter())) {
                return Optional.of(samlConfig);
            }
        }
        return Optional.empty();
    }

    @Override
    public LoginResp samlLogin(String idpName, HttpServletRequest request, HttpServletResponse response) {
        SamlConfigEntity samlConfig = samlConfigRepository.findByIdpName(idpName)
                .orElseThrow(() -> new SamlAuthFailedException("SAML IdP not supported: " + idpName));
        Saml2Settings settings = samlConfig.toSaml2Settings(appProperties.getBaseUrl(false));

        Auth auth;
        try {
            auth = getAuth(settings, request, response);
            auth.processResponse();
        } catch (Exception ex) {
            log.error("SAML authentication failed", ex);
            throw new SamlAuthFailedException("SAML authentication failed");
        }

        if (!auth.isAuthenticated() || !auth.getErrors().isEmpty()) {
            log.error("SAML authentication failed, errors: {}, reason: {}", auth.getErrors(), auth.getLastErrorReason());
            throw new SamlAuthFailedException("SAML authentication failed");
        }

        // assumes the IdP sends the email address as NameID (Keycloak client: Name ID format = email)
        String email = auth.getNameId();
        log.info("SAML login: idp={}, email={}", idpName, email);
        return getLoginResponse(email);
    }

    private LoginResp getLoginResponse(String email) {
        UsersAccountEntity account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new SamlAuthFailedException("No account for email: " + email));

        // same token as password login, so the JWT filter and role checks treat both the same way
        CustomUserDetails userDetails = new CustomUserDetails(account);
        String token = jwtService.generateToken(account.getId(), account.getEmail(), userDetails.getAuthorities());

        UserProfileResp profile = new UserProfileResp();
        profile.setId(account.getId());
        profile.setName(account.getUsername());
        profile.setEmail(account.getEmail());
        profile.setRole(account.getRole());

        LoginResp resp = new LoginResp();
        resp.setToken(token);
        resp.setUserProfile(profile);
        return resp;
    }

    private Auth getAuth(Saml2Settings settings, HttpServletRequest request, HttpServletResponse response) throws SettingsException {
        Auth auth = new Auth(settings, request, response);
        auth.setSamlMessageFactory(new CustomSamlMessageFactory());
        return auth;
    }
}
