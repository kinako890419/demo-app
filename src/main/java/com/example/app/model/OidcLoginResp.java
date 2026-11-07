package com.example.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/** Same token and user as {@link LoginResp}, plus the IdP's ID token for inspection. */
@Getter
@Builder
public class OidcLoginResp {

    @JsonProperty("token")
    private String token;

    @JsonProperty("user")
    private UserProfileResp userProfile;

    @JsonProperty("id_token")
    private String idToken;
}
