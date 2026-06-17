param(
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$DatabaseName = "stockwise_inventory",
    [string]$Username = "root",
    [SecureString]$Password,
    [string]$MySqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"

function Resolve-MySqlExe {
    param([string]$PreferredPath)

    if (Test-Path -LiteralPath $PreferredPath) {
        return (Resolve-Path -LiteralPath $PreferredPath).Path
    }

    $command = Get-Command mysql -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    throw "mysql.exe was not found. Install MySQL Server or add mysql.exe to PATH."
}

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

$resolvedMySqlExe = Resolve-MySqlExe -PreferredPath $MySqlExe
$plainPassword = ConvertFrom-SecureStringPlainText -SecureValue $Password
$oldPassword = $env:MYSQL_PWD

try {
    $env:MYSQL_PWD = $plainPassword
    $sql = "CREATE DATABASE IF NOT EXISTS ``$DatabaseName``; SELECT VERSION() AS mysql_version; SELECT SCHEMA_NAME AS database_name FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '$DatabaseName';"

    $sql | & $resolvedMySqlExe -h $HostName -P $Port -u $Username -N -B
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL connection failed with exit code $LASTEXITCODE."
    }

    Write-Host ""
    Write-Host "MySQL connection is working." -ForegroundColor Green
    Write-Host "Use this app command next:"
    Write-Host ".\run-existing-mysql.ps1"
}
finally {
    $env:MYSQL_PWD = $oldPassword
}
