package com.example.app.controller;

import com.example.app.common.models.UrlResponse;
import com.example.app.model.LoginResp;
import com.example.app.service.SamlAuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/saml")
public class SamlAuthController {

    private final SamlAuthService samlAuthService;

    @Operation(summary = "Get the SAML IdP login URL", description = "Picks the IdP by email domain.")
    @GetMapping("/idp-url")
    public UrlResponse getIdpUrl(@RequestParam String email,
                                 @RequestParam(required = false, defaultValue = "false") boolean isLocalDev) {
        return samlAuthService.getIdpUrlByEmail(email, isLocalDev);
    }

    @Operation(summary = "Assertion Consumer Service", description = "The IdP posts the SAMLResponse here; returns the app JWT.")
    @PostMapping("/login/{idpName}")
    public LoginResp login(@PathVariable String idpName, HttpServletRequest request, HttpServletResponse response) {
        return samlAuthService.samlLogin(idpName, request, response);
    }
}
