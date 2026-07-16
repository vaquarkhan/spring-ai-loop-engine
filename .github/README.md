# .github

GitHub Actions and repository automation for Spring AI Loop Engine.

## Contents

| Path | Purpose |
|------|---------|
| [workflows/build.yml](workflows/build.yml) | CI: checkout, JDK 21, `mvn verify` on push/PR to `main` |

## Local equivalent

```bash
mvn -B verify
```

Requires JDK 21+.

Additional workflows (release, snapshot publish, community-workflows integration) can be added here when the project is ready for Maven Central / Spring AI Community packaging.
