param(
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$DatabaseName = "stockwise_inventory",
    [string]$Username = "root",
    [SecureString]$Password
)

$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "load-env.ps1")

$env:DEBUG = "false"

function ConvertFrom-SecureStringPlainText {
    param([SecureString]$SecureValue)

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        if ($bstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
}

if (-not $Password) {
    $Password = Read-Host "Enter MySQL password for '$Username'@$HostName" -AsSecureString
}

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

$oldDbUrl = $env:DB_URL
$oldDbUsername = $env:DB_USERNAME
$oldDbPassword = $env:DB_PASSWORD
$oldJpaDdlAuto = $env:JPA_DDL_AUTO

try {
    $env:DB_URL = "jdbc:mysql://${HostName}:${Port}/${DatabaseName}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    $env:DB_USERNAME = $Username
    $env:DB_PASSWORD = ConvertFrom-SecureStringPlainText -SecureValue $Password
    $env:JPA_DDL_AUTO = "update"

    Write-Host "Starting Smart Invent with existing MySQL connection:"
    Write-Host "  Host: $HostName"
    Write-Host "  Port: $Port"
    Write-Host "  Database: $DatabaseName"
    Write-Host "  Username: $Username"
    Write-Host ""

    & $mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
}
finally {
    $env:DB_URL = $oldDbUrl
    $env:DB_USERNAME = $oldDbUsername
    $env:DB_PASSWORD = $oldDbPassword
    $env:JPA_DDL_AUTO = $oldJpaDdlAuto
}
