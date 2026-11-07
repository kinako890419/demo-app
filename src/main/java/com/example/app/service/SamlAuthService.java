package com.example.app.service;

import com.example.app.common.models.UrlResponse;
import com.example.app.entity.SamlConfigEntity;
import com.example.app.model.LoginResp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

public interface SamlAuthService {

    UrlResponse getIdpUrlByEmail(String email, boolean isLocalDev);

    UrlResponse getSamlIdpUrl(SamlConfigEntity samlConfig, boolean isLocalDev);

    Optional<SamlConfigEntity> getSamlConfigByDomain(String domain);

    LoginResp samlLogin(String idpName, HttpServletRequest request, HttpServletResponse response);
}
