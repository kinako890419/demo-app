package com.example.app.service.impl;

import com.example.app.common.config.AppProperties;
import com.example.app.common.exception.BadRequestException;
import com.example.app.common.exception.DataNotFoundException;
import com.example.app.common.exception.OidcAuthFailedException;
import com.example.app.common.models.UrlResponse;
import com.example.app.common.util.EmailUtils;
import com.example.app.entity.OidcConfigEntity;
import com.example.app.entity.UsersAccountEntity;
import com.example.app.model.OidcLoginResp;
import com.example.app.model.OidcParams;
import com.example.app.model.UserProfileResp;
import com.example.app.repository.OidcConfigRepository;
import com.example.app.repository.UsersAccountRepository;
import com.example.app.security.CustomUserDetails;
import com.example.app.security.jwt.JwtService;
import com.example.app.service.OidcAuthService;
import com.example.app.service.OidcParamsMaintainService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * OIDC Authorization Code flow with PKCE, ported from kinako890419/demo (dev).
 * Only existing, non-deleted accounts can sign in; the account is matched by the ID token's email claim.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OidcAuthServiceImpl implements OidcAuthService {

    private final AppProperties appProperties;
    private final OidcConfigRepository oidcConfigRepo;
    private final JwtService jwtService;
    private final OidcParamsMaintainService oidcParamsSvc;
    private final UsersAccountRepository usersAccountRepository;

    @Override
    public UrlResponse getIdpRedirectUrl(String idp, String email, boolean isLocalDev) {
        OidcConfigEntity oidcConfig;
        if (StringUtils.isNotBlank(email)) {
            String domain = EmailUtils.getDomain(email);
            oidcConfig = getOidcConfigByDomain(domain).orElseThrow(
                    () -> new DataNotFoundException("No IdP configured for email domain: " + domain));
        } else if (StringUtils.isNotBlank(idp)) {
            oidcConfig = oidcConfigRepo.findByIdpName(idp).orElseThrow(
                    () -> new DataNotFoundException("OIDC provider not supported: " + idp));
        } else {
            throw new BadRequestException("Either idp or email is required");
        }

        return getOidcIdpRedirectUrl(oidcConfig, email, isLocalDev);
    }

    @Override
    public UrlResponse getOidcIdpRedirectUrl(OidcConfigEntity oidcConfig, String loginHint, boolean isLocalDev) {
        if (StringUtils.isAnyBlank(oidcConfig.getIssuerUrl(), oidcConfig.getClientId())) {
            throw new IllegalStateException("OIDC IdP is not fully configured: " + oidcConfig.getIdpName());
        }

        State state = new State();
        Nonce nonce = new Nonce();
        CodeVerifier codeVerifier = new CodeVerifier();

        try {
            URI authzEndpoint = resolveMetadata(oidcConfig).getAuthorizationEndpointURI();

            oidcParamsSvc.store(state.getValue(), nonce.getValue(), codeVerifier.getValue());

            AuthenticationRequest request = new AuthenticationRequest.Builder(
                    new ResponseType(ResponseType.Value.CODE),
                    Scope.parse(oidcConfig.getOidcScope()),
                    new ClientID(oidcConfig.getClientId()),
                    new URI(getCallbackUrl(oidcConfig, isLocalDev)))
                    .endpointURI(authzEndpoint)
                    .state(state)
                    .nonce(nonce)
                    .codeChallenge(codeVerifier, CodeChallengeMethod.S256)  // PKCE
                    .loginHint(loginHint) // pre-fill the email on the IdP login page
                    .build();

            return UrlResponse.builder().url(request.toURI().toString()).build();

        } catch (Exception e) {
            oidcParamsSvc.remove(state.getValue());
            log.error("Get OIDC IdP url failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Get OIDC IdP url failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<OidcConfigEntity> getOidcConfigByDomain(String domain) {
        for (OidcConfigEntity oidcConfig : oidcConfigRepo.findAll()) {
            if (oidcConfig.getDomainFilter() != null && domain.matches(oidcConfig.getDomainFilter())) {
                return Optional.of(oidcConfig);
            }
        }
        return Optional.empty();
    }

    @Override
    public OidcLoginResp handleOidcCallback(String providerName, String code, String state,
                                            String error, String errorDescription, boolean isLocalDev) {
        if (StringUtils.isNotEmpty(error) || StringUtils.isNotEmpty(errorDescription)) {
            oidcParamsSvc.remove(state);
            throw new OidcAuthFailedException("OIDC callback error: " + error + " - " + errorDescription);
        }
        if (StringUtils.isBlank(code)) {
            throw new OidcAuthFailedException("OIDC callback has no authorization code.");
        }

        // one-time use: the params are deleted as they are read
        OidcParams params = oidcParamsSvc.consumeParams(state)
                .orElseThrow(() -> new OidcAuthFailedException("Invalid or expired state."));

        Nonce storedNonce = new Nonce(params.getNonce());
        CodeVerifier verifier = new CodeVerifier(params.getCodeVerifier());

        OidcConfigEntity oidcConfig = oidcConfigRepo.findByIdpName(providerName)
                .orElseThrow(() -> new OidcAuthFailedException("OIDC provider not supported: " + providerName));

        TokenResponse tokenResponse = exchangeCodeForToken(oidcConfig, code, verifier, isLocalDev);

        if (!tokenResponse.indicatesSuccess()) {
            String errResp = tokenResponse.toErrorResponse().getErrorObject().getDescription();
            throw new OidcAuthFailedException("Token exchange failed: " + errResp);
        }

        OIDCTokenResponse successResponse = (OIDCTokenResponse) tokenResponse.toSuccessResponse();
        JWT idToken = successResponse.getOIDCTokens().getIDToken();

        IDTokenClaimsSet claims = validateIdToken(oidcConfig, idToken, storedNonce);

        Object emailClaim = claims.getClaim("email");
        if (emailClaim == null) {
            throw new OidcAuthFailedException("ID token has no email claim.");
        }
        String mail = emailClaim.toString();
        log.info("OIDC login: idp={}, subject={}, email={}", providerName, claims.getSubject().getValue(), mail);

        UsersAccountEntity account = usersAccountRepository.findByEmail(mail).orElseThrow(
                () -> new OidcAuthFailedException("No account for email: " + mail));

        // same token as password login, so the JWT filter and role checks treat both the same way
        CustomUserDetails userDetails = new CustomUserDetails(account);
        String token = jwtService.generateToken(account.getId(), account.getEmail(), userDetails.getAuthorities());

        UserProfileResp profile = new UserProfileResp();
        profile.setId(account.getId());
        profile.setName(account.getUsername());
        profile.setEmail(account.getEmail());
        profile.setRole(account.getRole());

        return OidcLoginResp.builder()
                .token(token)
                .userProfile(profile)
                .idToken(idToken.getParsedString())
                .build();
    }

    private TokenResponse exchangeCodeForToken(OidcConfigEntity oidcConfig, String code,
                                               CodeVerifier verifier, boolean isLocalDev) {
        ClientAuthentication clientAuth = getSupportedTokenAuthMethod(oidcConfig);

        try {
            AuthorizationGrant codeGrant = new AuthorizationCodeGrant(
                    new AuthorizationCode(code), new URI(getCallbackUrl(oidcConfig, isLocalDev)), verifier);

            URI tokenEndpoint = resolveMetadata(oidcConfig).getTokenEndpointURI();

            TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, codeGrant,
                    Scope.parse(oidcConfig.getOidcScope()));

            return OIDCTokenResponseParser.parse(request.toHTTPRequest().send());

        } catch (Exception e) {
            log.error("Token exchange failed: {}", e.getMessage(), e);
            throw new OidcAuthFailedException("Token exchange failed: " + e.getMessage());
        }
    }

    private ClientAuthentication getSupportedTokenAuthMethod(OidcConfigEntity oidcConfig) {
        List<ClientAuthenticationMethod> authMethods;
        try {
            authMethods = resolveMetadata(oidcConfig).getTokenEndpointAuthMethods();
        } catch (GeneralException | IOException e) {
            throw new OidcAuthFailedException("Get token auth methods failed: " + e.getMessage());
        }

        ClientID clientId = new ClientID(oidcConfig.getClientId());
        Secret secret = new Secret(oidcConfig.getClientSecret());
        // the spec default is client_secret_basic when the IdP does not list any methods
        if (authMethods == null) {
            return new ClientSecretBasic(clientId, secret);
        } else if (authMethods.contains(ClientAuthenticationMethod.CLIENT_SECRET_POST)) {
            return new ClientSecretPost(clientId, secret);
        } else if (authMethods.contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)) {
            return new ClientSecretBasic(clientId, secret);
        } else {
            throw new OidcAuthFailedException("No supported token endpoint auth method: " + authMethods);
        }
    }

    private IDTokenClaimsSet validateIdToken(OidcConfigEntity oidcConfig, JWT idToken, Nonce expectedNonce) {
        try {
            URI jwksEndpoint = resolveMetadata(oidcConfig).getJWKSetURI();

            IDTokenValidator validator = new IDTokenValidator(
                    new Issuer(oidcConfig.getIssuerUrl()),
                    new ClientID(oidcConfig.getClientId()),
                    JWSAlgorithm.RS256,
                    jwksEndpoint.toURL()
            );
            return validator.validate(idToken, expectedNonce);

        } catch (BadJOSEException | JOSEException e) {
            log.error("ID token validation failed: {}", e.getMessage(), e);
            throw new OidcAuthFailedException("ID token validation failed: " + e.getMessage());
        } catch (GeneralException | IOException e) {
            log.error("Failed to resolve OIDC provider metadata: {}", e.getMessage(), e);
            throw new OidcAuthFailedException("Failed to resolve OIDC provider metadata: " + e.getMessage());
        }
    }

    private OIDCProviderMetadata resolveMetadata(OidcConfigEntity oidcConfig) throws GeneralException, IOException {
        return OIDCProviderMetadata.resolve(new Issuer(oidcConfig.getIssuerUrl()));
    }

    /**
     * The isLocalDev flag is carried on the callback URL so the callback can rebuild the exact
     * redirect_uri for the token exchange.
     */
    private String getCallbackUrl(OidcConfigEntity oidcConfig, boolean isLocalDev) {
        String callbackUrl = appProperties.getBaseUrl(isLocalDev) + oidcConfig.getCallbackPath();
        return isLocalDev ? callbackUrl + "?isLocalDev=true" : callbackUrl;
    }
}
