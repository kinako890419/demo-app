package com.example.app.controller;

import com.example.app.common.models.UrlResponse;
import com.example.app.model.OidcLoginResp;
import com.example.app.service.OidcAuthService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oidc")
public class OidcAuthController {

    private final OidcAuthService oidcAuthService;

    @Operation(summary = "Get the IdP login URL", description = "Pass email to pick the IdP by email domain, or idp to pick it by name.")
    @GetMapping("/login")
    public UrlResponse login(@RequestParam(required = false) String idp,
                             @RequestParam(required = false) String email,
                             @RequestParam(required = false, defaultValue = "false") boolean isLocalDev) {
        return oidcAuthService.getIdpRedirectUrl(idp, email, isLocalDev);
    }

    @Operation(summary = "IdP redirect target", description = "Exchanges the code, validates the ID token and returns the app JWT.")
    @GetMapping("/{provider}/callback")
    public OidcLoginResp handleOidcCallback(@PathVariable String provider,
                                            @RequestParam(required = false) String code,
                                            @RequestParam String state,
                                            @RequestParam(required = false) String error,
                                            @RequestParam(name = "error_description", required = false) String errorDescription,
                                            @RequestParam(required = false, defaultValue = "false") boolean isLocalDev) {
        return oidcAuthService.handleOidcCallback(provider, code, state, error, errorDescription, isLocalDev);
    }
}
