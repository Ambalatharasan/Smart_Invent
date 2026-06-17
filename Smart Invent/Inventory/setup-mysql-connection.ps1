param(
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$AdminUser = "root",
    [SecureString]$AdminPassword,
    [string]$DatabaseName = "stockwise_inventory",
    [string]$AppUser = "stockwise",
    [SecureString]$AppPassword,
    [string]$MySqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [switch]$SkipSeed
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

function SqlString {
    param([string]$Value)
    return $Value.Replace("'", "''")
}

function SqlIdentifier {
    param([string]$Value)

    if ($Value -notmatch '^[A-Za-z0-9_]+$') {
        throw "Only letters, numbers, and underscores are allowed in SQL identifiers. Invalid value: $Value"
    }

    return "``$Value``"
}

function Invoke-MySqlSql {
    param(
        [string]$Sql,
        [string]$User,
        [SecureString]$Password,
        [string]$Database
    )

    $plainPassword = ConvertFrom-SecureStringPlainText -SecureValue $Password
    $oldPassword = $env:MYSQL_PWD

    try {
        $env:MYSQL_PWD = $plainPassword
        $args = @("-h", $HostName, "-P", $Port.ToString(), "-u", $User)
        if ($Database) {
            $args += "--database=$Database"
        }

        $Sql | & $script:ResolvedMySqlExe @args
        if ($LASTEXITCODE -ne 0) {
            throw "mysql.exe failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        $env:MYSQL_PWD = $oldPassword
    }
}

$script:ResolvedMySqlExe = Resolve-MySqlExe -PreferredPath $MySqlExe

if (-not $AdminPassword) {
    $AdminPassword = Read-Host "Enter MySQL password for admin user '$AdminUser'" -AsSecureString
}

if (-not $AppPassword) {
    $AppPassword = Read-Host "Enter password to create/use for app user '$AppUser'" -AsSecureString
}

$plainAppPassword = ConvertFrom-SecureStringPlainText -SecureValue $AppPassword
$db = SqlIdentifier -Value $DatabaseName
$appUserSql = SqlString -Value $AppUser
$appPasswordSql = SqlString -Value $plainAppPassword

$connectionSql = @"
CREATE DATABASE IF NOT EXISTS $db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '$appUserSql'@'localhost' IDENTIFIED BY '$appPasswordSql';
ALTER USER '$appUserSql'@'localhost' IDENTIFIED BY '$appPasswordSql';

CREATE USER IF NOT EXISTS '$appUserSql'@'%' IDENTIFIED BY '$appPasswordSql';
ALTER USER '$appUserSql'@'%' IDENTIFIED BY '$appPasswordSql';

GRANT ALL PRIVILEGES ON $db.* TO '$appUserSql'@'localhost';
GRANT ALL PRIVILEGES ON $db.* TO '$appUserSql'@'%';
FLUSH PRIVILEGES;
"@

Write-Host "Creating Smart Invent MySQL database and application user..."
Invoke-MySqlSql -Sql $connectionSql -User $AdminUser -Password $AdminPassword

$schemaPath = Join-Path $PSScriptRoot "src\main\resources\db\mysql\schema.sql"
$seedPath = Join-Path $PSScriptRoot "src\main\resources\db\mysql\seed-data.sql"

Write-Host "Loading schema from $schemaPath..."
$schemaSql = (Get-Content -Raw -Path $schemaPath).Replace("stockwise_inventory", $DatabaseName)
Invoke-MySqlSql -Sql $schemaSql -User $AdminUser -Password $AdminPassword

if (-not $SkipSeed) {
    Write-Host "Loading seed data from $seedPath..."
    $seedSql = (Get-Content -Raw -Path $seedPath).Replace("stockwise_inventory", $DatabaseName)
    Invoke-MySqlSql -Sql $seedSql -User $AdminUser -Password $AdminPassword
}

Write-Host "Verifying application user connection..."
$verifySql = "SELECT DATABASE() AS database_name, CURRENT_USER() AS connected_user;"
Invoke-MySqlSql -Sql $verifySql -User $AppUser -Password $AppPassword -Database $DatabaseName

Write-Host ""
Write-Host "MySQL connection is ready for Smart Invent." -ForegroundColor Green
Write-Host "Profile: mysql"
Write-Host "DB_URL=jdbc:mysql://${HostName}:${Port}/${DatabaseName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
Write-Host "DB_USERNAME=$AppUser"
Write-Host "DB_PASSWORD=<the app user password you entered>"
