# Backend Smoke Tests

Minimal runtime smoke set for the backend:

1. `register -> login` returns a valid JWT token
2. JWT grants access to a protected endpoint
3. Lift CRUD round trip works end-to-end

Smoke test entry point:

- `com.smartlift.integrational.BackendSmokeIntegrationTest`

Run only smoke tests:

```bash
mvn test -Psmoke
```

Run the full backend suite:

```bash
mvn test
```

Requirements:

- PostgreSQL must be available on `localhost:5434`
- test profile uses the local database `smartlift`
