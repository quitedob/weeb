# Weeb

Spring Boot 3 / Java 17 backend with a Vue 3 frontend in `Vue/`.

## Build and verify

```powershell
.\mvnw.cmd clean test
cd Vue
npm ci
npm run test:run
npm run build
```

On POSIX shells use `./mvnw` in place of `mvnw.cmd`. The wrapper bootstraps Maven 3.9.9. `Vue/package.json` is the frontend manifest.

## Run locally

Export the environment variables documented in [.env.example](.env.example), including a new `JWT_SECRET` and the database password. Provide MySQL and Redis. Elasticsearch is optional and disabled by default; enable it with `ELASTICSEARCH_ENABLED=true` only when its service is configured. The `.env.example` file is an example, not automatically loaded backend configuration.

```powershell
.\mvnw.cmd spring-boot:run
```

In a separate terminal:

```powershell
cd Vue
npm run dev
```

During development, Vite proxies `/api` requests to `VITE_API_BASE_URL` (default `http://localhost:8080`). The chat store opens SockJS directly at `VITE_WS_URL` (default `http://localhost:8080/ws`); the configured Vite `/ws` proxy is not used by that default. Use a browser-reachable full HTTP(S) SockJS endpoint, including `/ws`, when configuring `VITE_WS_URL`. Production HTTP requests use `VITE_API_BASE_URL` directly. Only variables prefixed `VITE_` are exposed to client code; do not put credentials in them.

Accounts are created through registration. An operator must explicitly provision an administrator using the stored user type; no default password or username convention grants administrator privileges. Password-reset email requires SMTP configuration and the correct frontend reset URL.

See [backend API documentation](docs/backend.md), [frontend documentation](docs/frontend.md), [store documentation](docs/stores.md), [SQL deployment notes](src/main/resources/sql/README.md), and [credential remediation](docs/operations.md). The original audit is in [docs/report.md](docs/report.md); work and verification are tracked in [docs/remediation-plan.md](docs/remediation-plan.md).
