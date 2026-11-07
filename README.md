spring boot 4

## SSO login: OIDC and SAML (local Keycloak)

`dockerfiles/compose.yaml` starts Keycloak 26.5.1 on http://localhost:8888 (admin console: `admin` / `admin`).
On every start it imports `dockerfiles/keycloak/app-realm.json` (skipped if the realm already exists), so the
`app` realm, the `app-oidc` (OIDC) and `saml-test` (SAML) clients and the test users come back after
`docker compose down`. The OIDC client secret (`KC_OIDC_CLIENT_SECRET`) and the realm signing key
(`KC_SIGNING_PRIVATE_KEY`) come from `dockerfiles/.env`. The signing key is pinned so its certificate,
which the app needs to verify SAML responses, stays the same.

On startup `CommandLineAppStartupRunner` seeds:
- the `keycloak` row in `OIDC_CONFIG` (domain filter `test\.com`) from the `seed.oidc.keycloak.*` properties
- the `google` row (domain filter `gmail\.com`), only if `seed.oidc.google.client-id` is set
- the `saml-test` row in `SAML_CONFIG` (domain filter `test2\.com`) from the `seed.saml.saml-test.*` properties
- the SSO-only accounts `test@test.com` and `test@test2.com` (role `USER`, no password)

Existing rows are never overwritten, only blank fields are filled.

| Keycloak user | Password | Email | App account |
|---------------|----------|-------|-------------|
| `testuser` | `testpass` | `test@test.com` | `test@test.com` (USER), signs in via OIDC |
| `samluser` | `testpass` | `test@test2.com` | `test@test2.com` (USER), signs in via SAML |

`GET /api/v1/sso/login?email=...` returns the IdP URL for either protocol (SAML wins if both match the domain).

OIDC flow:
1. `GET /api/v1/oidc/login?email=test@test.com` (or `?idp=keycloak`) returns `{"url": ...}`.
2. Open the URL in a browser and sign in on Keycloak.
3. Keycloak redirects to `/api/v1/oidc/keycloak/callback`, which returns
   `{"token", "user", "id_token"}`. `token` is the same app JWT as `POST /api/auth/login`.

Only existing accounts can sign in (matched by the ID token's `email` claim). The state, nonce and PKCE
verifier are kept in Redis as `oidc:state:{state}` for 5 minutes and can be used once.

SAML flow (onelogin java-saml, ported from kinako890419/demo as is):
1. `GET /api/v1/saml/idp-url?email=test@test2.com` returns `{"url": ...}` with the `SAMLRequest`.
2. Open the URL in a browser and sign in on Keycloak.
3. Keycloak posts the signed `SAMLResponse` (NameID = email) to `/api/v1/saml/login/saml-test`, which returns
   `{"token", "user"}`.

Known gaps in the SAML port: `CustomSamlResponse` skips the Destination, Recipient and Audience checks, and the
AuthnRequest ID is not stored, so `InResponseTo` is not checked and a captured response can be replayed until it
expires. The jakarta copies of onelogin's `Auth`, `ServletUtils` and `SamlMessageFactory` in
`src/main/java/com/onelogin/saml2` replace the javax versions inside `java-saml-core`.
