package com.example.app.service;

import com.example.app.model.OidcParams;

import java.util.Optional;

public interface OidcParamsMaintainService {

    void store(String state, String nonce, String codeVerifier);

    /** Returns the params for this state and deletes them, so a state can only be used once. */
    Optional<OidcParams> consumeParams(String state);

    void remove(String state);
}
