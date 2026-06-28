## 1. nginx configuration

- [x] 1.1 Create `devops/local/nginx/` directory for proxy config
- [x] 1.2 Add an nginx config template (e.g. `default.conf.template`) defining a `limit_req_zone` keyed on `$binary_remote_addr` with a `${RATE_LIMIT}` rate using a named shared-memory zone
- [x] 1.3 Add a `server` block listening on port 80 with a `location /` that `proxy_pass`es to `http://backend:8080`
- [x] 1.4 Apply `limit_req` with `burst=${RATE_BURST}` and `nodelay` to the proxied location
- [x] 1.5 Set `limit_req_status 429;` so rejected requests return 429 instead of the default 503
- [x] 1.6 Set `proxy_set_header` for `Host`, `X-Forwarded-For` (via `$proxy_add_x_forwarded_for`), and `X-Forwarded-Proto`
- [x] 1.7 Ensure `envsubst` only substitutes `RATE_LIMIT`/`RATE_BURST` and not nginx runtime variables (escape nginx `$` vars or set `NGINX_ENVSUBST_FILTER`)

## 2. Compose wiring

- [x] 2.1 Add `RATE_LIMIT` and `RATE_BURST` to `devops/local/.env` with conservative defaults (e.g. `10r/s`, `20`)
- [x] 2.2 Add an `nginx` service (`nginx:alpine`) to `devops/local/docker-compose.yml` publishing `80:80`
- [x] 2.3 Mount the config template read-only into `/etc/nginx/templates/` and pass `RATE_LIMIT`/`RATE_BURST` as environment variables
- [x] 2.4 Add `depends_on: backend` to the `nginx` service
- [x] 2.5 Remove the `8080:8080` host port mapping from the `backend` service so it is only reachable on the internal network
- [x] 2.6 Confirm `backend` and `nginx` share the same compose network and nginx can resolve `backend` by service name

## 3. Verification

- [x] 3.1 `docker compose -f devops/local/docker-compose.yml up -d --build` and confirm all services start healthy
- [x] 3.2 Verify a normal request via `http://localhost/` reaches the app and returns the expected response
- [x] 3.3 Verify Swagger UI is reachable through the proxy at `http://localhost/swagger-ui.html`
- [x] 3.4 Verify rapid repeated requests beyond rate+burst return HTTP `429`
- [x] 3.5 Verify the backend is no longer directly reachable at `http://localhost:8080` (connection refused)
- [x] 3.6 Verify the backend receives `X-Forwarded-For` with the real client IP

## 4. Documentation

- [x] 4.1 Update `devops/local/README.md`: new entry point (`http://localhost`), the `nginx` service, and how to tune `RATE_LIMIT`/`RATE_BURST`
- [x] 4.2 Update the `## Local Services` table in `CLAUDE.md` to reflect the new API entry point through nginx
