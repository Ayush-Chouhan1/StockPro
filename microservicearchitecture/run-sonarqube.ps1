param(
    [string]$SonarUrl = $(if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { "http://localhost:9000" }),
    [string]$SonarToken = $env:SONAR_TOKEN,
    [switch]$PromptForToken,
    [switch]$SkipTests,
    [switch]$SkipBackend,
    [switch]$StopOnFailure
)

$rootScript = Join-Path $PSScriptRoot "..\run-sonarqube.ps1"

$parameters = @{
    SonarUrl = $SonarUrl
    SkipFrontend = $true
}

if ($SonarToken) {
    $parameters.SonarToken = $SonarToken
}

if ($PromptForToken -or [string]::IsNullOrWhiteSpace($SonarToken)) {
    $parameters.PromptForToken = $true
}

if ($SkipTests) {
    $parameters.SkipTests = $true
}

if ($SkipBackend) {
    $parameters.SkipBackend = $true
}

if ($StopOnFailure) {
    $parameters.StopOnFailure = $true
}

& $rootScript @parameters
exit $LASTEXITCODE
