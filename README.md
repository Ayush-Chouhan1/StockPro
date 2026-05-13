*STOCK PRO*

## Dockerized Backend

Each backend microservice has its own Dockerfile under `microservicearchitecture/<service-name>/Dockerfile`.
The Compose stack builds those service images individually and starts MySQL, RabbitMQ, Eureka, the API gateway, and all backend services.

```powershell
cd microservicearchitecture
docker compose up --build
```

Useful URLs:

- API gateway: `http://localhost:8080`
- Eureka dashboard: `http://localhost:8761`
- RabbitMQ dashboard: `http://localhost:15672` (`stockpro` / `stockpro123`)
- MySQL: `localhost:3306` (`root` / `root`)

To stop the stack:

```powershell
docker compose down
```

## SonarQube One-Click Scan

This repo includes a root runner for scanning the Java microservices and React frontend with SonarQube.

### Before running

1. Start SonarQube in Docker and make sure it is reachable on `http://localhost:9000`
2. Generate a SonarQube token
3. Set the token in PowerShell:

```powershell
$env:SONAR_TOKEN="your_token_here"
```

Optional:

```powershell
$env:SONAR_HOST_URL="http://localhost:9000"
```

For frontend analysis, install SonarScanner CLI or make sure `npx` is available. The runner uses `sonar-scanner` first and falls back to `npx --yes sonar-scanner`.

### Run from the root folder

Terminal:

```powershell
.\run-sonarqube.ps1
```

Windows one-click launcher:

```text
run-sonarqube.bat
```

From the backend folder:

```powershell
cd microservicearchitecture
.\run-sonar.bat
```

or:

```powershell
.\run-sonarqube.ps1
```

### Useful options

```powershell
.\run-sonarqube.ps1 -SkipTests
.\run-sonarqube.ps1 -SkipFrontend
.\run-sonarqube.ps1 -SkipBackend
.\run-sonarqube.ps1 -StopOnFailure
.\run-sonarqube.ps1 -SonarUrl "http://localhost:9000"
```

### Notes

- The script scans each microservice one by one
- It installs `auth-service` into the local Maven cache first because other services depend on it
- `eureka-server` is included in backend scanning
- If a service test suite fails, the script records the failure and continues with the next service
- Frontend coverage is generated with `npm run coverage` and read from `stockpro-frontend/coverage/lcov.info`
- Frontend analysis uses `stockpro-frontend/sonar-project.properties`
- Use `.\run-sonarqube.ps1 -SkipTests` if you want a build-and-scan run without test execution
- The batch files prompt for the SonarQube token if `SONAR_TOKEN` is not already set
- Do not commit SonarQube tokens. Use `SONAR_TOKEN` or the `-SonarToken` parameter
