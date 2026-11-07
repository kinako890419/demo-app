spring boot 4

## OIDC login (local Keycloak)

`dockerfiles/compose.yaml` starts Keycloak 26.5.1 on http://localhost:8888 (admin console: `admin` / `admin`).
On every start it imports `dockerfiles/keycloak/app-realm.json` (skipped if the realm already exists), so the
`app` realm, the `app-oidc` client and the test user come back after `docker compose down`.
The client secret comes from `KC_OIDC_CLIENT_SECRET` in `dockerfiles/.env`.

On startup `CommandLineAppStartupRunner` seeds:
- the `keycloak` row in `OIDC_CONFIG` (domain filter `test\.com`) from the `seed.oidc.keycloak.*` properties
- the `google` row (domain filter `gmail\.com`), only if `seed.oidc.google.client-id` is set
- the SSO-only account `test@test.com` (role `USER`, no password)

Existing rows are never overwritten, only blank fields are filled.

| Keycloak user | Password | Email | App account |
|---------------|----------|-------|-------------|
| `testuser` | `testpass` | `test@test.com` | `test@test.com` (USER) |

Flow:
1. `GET /api/v1/oidc/login?email=test@test.com` (or `?idp=keycloak`) returns `{"url": ...}`.
2. Open the URL in a browser and sign in on Keycloak.
3. Keycloak redirects to `/api/v1/oidc/keycloak/callback`, which returns
   `{"token", "user", "id_token"}`. `token` is the same app JWT as `POST /api/auth/login`.

Only existing accounts can sign in (matched by the ID token's `email` claim). The state, nonce and PKCE
verifier are kept in Redis as `oidc:state:{state}` for 5 minutes and can be used once.
