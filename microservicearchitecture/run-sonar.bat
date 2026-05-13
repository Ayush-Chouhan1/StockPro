@echo off
REM ============================================================
REM StockPro - SonarQube Analysis Script
REM Run this from the microservicearchitecture folder
REM ============================================================

REM STEP 1: Paste your SonarQube token here
set SONAR_TOKEN=squ_17bd87e1d0f4173e2fc111ccfa8df7e0dd1af9c1

REM STEP 2: Make sure SonarQube is running at http://localhost:9000
REM         and all services compile successfully before running this

echo.
echo ============================================================
echo  StockPro SonarQube Analysis - Starting
echo ============================================================

set SERVICES=alert-service auth-service movement-service payment-service product-service purchase-service report-service supplier-service warehouse-service

for %%s in (%SERVICES%) do (
    echo.
    echo ------------------------------------------------------------
    echo  Analyzing: %%s
    echo ------------------------------------------------------------
    cd %%s
    call mvn clean verify sonar:sonar -Dsonar.token=%SONAR_TOKEN% -DskipTests=false
    if errorlevel 1 (
        echo [ERROR] %%s analysis failed!
        cd ..
        pause
        exit /b 1
    )
    cd ..
    echo [SUCCESS] %%s analyzed successfully
)

echo.
echo ============================================================
echo  All services analyzed!
echo  Open http://localhost:9000/projects to view results
echo ============================================================
pause
