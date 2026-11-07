package com.example.app.model;

import lombok.Builder;
import lombok.Getter;

/** Values kept in Redis between the login redirect and the callback. */
@Getter
@Builder
public class OidcParams {
    private final String nonce;
    private final String codeVerifier;
}
