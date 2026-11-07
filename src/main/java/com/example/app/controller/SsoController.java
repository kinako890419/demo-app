package com.example.app.controller;

import com.example.app.common.models.UrlResponse;
import com.example.app.service.SsoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sso")
public class SsoController {

    private final SsoService ssoService;

    @Operation(summary = "Get the IdP login URL for an email", description = "SAML wins when both a SAML and an OIDC IdP match the domain.")
    @GetMapping("/login")
    public UrlResponse ssoLogin(@RequestParam String email,
                                @RequestParam(required = false, defaultValue = "false") boolean isLocalDev) {
        return ssoService.ssoLogin(email, isLocalDev);
    }
}
