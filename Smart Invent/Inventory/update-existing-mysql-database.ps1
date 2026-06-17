param(
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$AdminUser = "root",
    [SecureString]$AdminPassword,
    [string]$DatabaseName = "testdb",
    [switch]$SeedDemoData,
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

function Assert-SqlIdentifier {
    param([string]$Value)

    if ($Value -notmatch '^[A-Za-z0-9_]+$') {
        throw "Only letters, numbers, and underscores are allowed in database names. Invalid value: $Value"
    }
}

function Invoke-MySqlSql {
    param([string]$Sql)

    $plainPassword = ConvertFrom-SecureStringPlainText -SecureValue $AdminPassword
    $oldPassword = $env:MYSQL_PWD

    try {
        $env:MYSQL_PWD = $plainPassword
        $Sql | & $script:ResolvedMySqlExe -h $HostName -P $Port -u $AdminUser
        if ($LASTEXITCODE -ne 0) {
            throw "mysql.exe failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        $env:MYSQL_PWD = $oldPassword
    }
}

Assert-SqlIdentifier -Value $DatabaseName
$script:ResolvedMySqlExe = Resolve-MySqlExe -PreferredPath $MySqlExe

if (-not $AdminPassword) {
    $AdminPassword = Read-Host "Enter MySQL password for admin user '$AdminUser'" -AsSecureString
}

$schemaPath = Join-Path $PSScriptRoot "src\main\resources\db\mysql\schema.sql"
$seedPath = Join-Path $PSScriptRoot "src\main\resources\db\mysql\seed-data.sql"
$schemaSql = (Get-Content -Raw -Path $schemaPath).Replace("stockwise_inventory", $DatabaseName)

Write-Host "Applying Smart Invent schema to MySQL database '$DatabaseName' on ${HostName}:${Port}..."
Invoke-MySqlSql -Sql $schemaSql

if ($SeedDemoData) {
    $seedSql = (Get-Content -Raw -Path $seedPath).Replace("stockwise_inventory", $DatabaseName)
    Write-Host "Loading demo seed data into '$DatabaseName'..."
    Invoke-MySqlSql -Sql $seedSql
}

Write-Host ""
Write-Host "Database structure is ready." -ForegroundColor Green
Write-Host "Run the app with:"
Write-Host ".\run-existing-mysql.ps1 -DatabaseName $DatabaseName -Username $AdminUser"
