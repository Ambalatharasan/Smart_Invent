$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "load-env.ps1")

$env:DEBUG = "false"

function Resolve-MavenCommand {
    $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCommand) {
        return $mvnCommand.Source
    }

    if ($env:MAVEN_HOME) {
        $mavenHomeCommand = Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
        if (Test-Path -LiteralPath $mavenHomeCommand) {
            return $mavenHomeCommand
        }
    }

    $bundledMaven = Join-Path $PSScriptRoot "..\..\..\tools\apache-maven-3.9.16\bin\mvn.cmd"
    if (Test-Path -LiteralPath $bundledMaven) {
        return (Resolve-Path -LiteralPath $bundledMaven).Path
    }

    return $null
}

$mvn = Resolve-MavenCommand
if (-not $mvn) {
    Write-Host "Maven is required to run this Spring Boot backend." -ForegroundColor Yellow
    Write-Host "Install Maven, set MAVEN_HOME, or keep the bundled Maven under C:\Users\rmamb\Documents\Codex\tools." -ForegroundColor Yellow
    exit 1
}

& $mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
