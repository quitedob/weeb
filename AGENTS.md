# Repository Guidelines

## Project Structure & Module Organization
- **Backend**: Java 17 Spring Boot app in `src/main/java/com/web`, organized by layer (controllers, service, repository, security, etc.). Shared configs and SQL/Mapper files live in `src/main/resources` (`application.yml`, `application-ai.properties`, `sql/`, `Mapper/`, `db/`, `es/`).
- **Frontend**: Vite-powered Vue 3 client in `Vue/src` with shared constants under `Vue/Constant` and static assets in `Vue/public`. Built assets are generated into `Vue/dist`.
- **Tests**: Backend integration, mapper, and performance suites sit in `src/test/java/com/web`; frontend component/unit tests live alongside source in `Vue/src` and run via Vitest.

## Build, Test, and Development Commands
- **Backend**: `mvnw.cmd clean package` (Windows) or `./mvnw clean package` builds the Spring Boot jar in `target/`. `mvnw.cmd spring-boot:run` starts the app with local configs.
- **Frontend**: From `Vue/`, run `npm install` once, then `npm run dev` for local Vite dev server, `npm run build` for production assets, and `npm run preview` to sanity-check the build.
- **Combined**: Launch backend first, then serve frontend (`npm run dev`) and point it at the API host configured in `Vue/src/utils/request.ts` if adjustments are needed.

## Coding Style & Naming Conventions
- Follow Spring conventions: 4-space indentation, PascalCase for classes (`UserServiceImpl`), camelCase for methods/fields, and suffix interfaces/DTOs/VOs (`UserMapper`, `LoginRequestDto`, `ArticleVo`).
- Prefer Lombok annotations already used in the codebase (e.g., `@Slf4j`, `@Data`) instead of manual boilerplate.
- Frontend uses script setup with TypeScript and 2-space indentation; keep component files PascalCase (`ArticleList.vue`) and stores in `*.store.ts`.

## Testing Guidelines
- Backend tests use JUnit 5 via `mvnw.cmd test`; name new classes with the `*Test` suffix and co-locate fixtures under `src/test/resources` if needed. Aim to cover mapper queries and security/business logic paths touched by the change.
- Frontend tests run with `npm run test` (watch) or `npm run test:run`. Co-locate `.spec.ts` files next to components and stub network calls with Vitest mocks to avoid hitting real services.
- For features spanning both stacks, document manual verification steps in the PR if automated coverage is impractical.

## Commit & Pull Request Guidelines
- Recent Git history uses simple version tags (`version 0.1`); move toward imperative, descriptive summaries (e.g., `feat: add elasticsearch article sync`) so reviewers understand scope at a glance.
- Keep commits focused; include configuration or schema changes with the code that depends on them.
- Pull requests should link relevant issues, outline backend/frontend impacts, note config/env updates (`application.yml`, `Vue/.env`), and attach screenshots or terminal output for UI- or API-facing changes.

## Configuration & Secrets
- Never commit real credentials. Use `.env` files or Spring config placeholders and document required keys in `README.md` or the PR.
- When modifying datasource, Redis, or Elasticsearch settings, update the sample values in `application.yml` and mention migration steps for `src/main/resources/sql/` artifacts.
