package com.example.app.service;

import com.example.app.common.models.UrlResponse;
import com.example.app.entity.OidcConfigEntity;
import com.example.app.model.OidcLoginResp;

import java.util.Optional;

public interface OidcAuthService {

    /** Picks the IdP by email domain when {@code email} is given, otherwise by {@code idp} name. */
    UrlResponse getIdpRedirectUrl(String idp, String email, boolean isLocalDev);

    UrlResponse getOidcIdpRedirectUrl(OidcConfigEntity oidcConfig, String loginHint, boolean isLocalDev);

    Optional<OidcConfigEntity> getOidcConfigByDomain(String domain);

    OidcLoginResp handleOidcCallback(String providerName, String code, String state,
                                     String error, String errorDescription, boolean isLocalDev);
}
