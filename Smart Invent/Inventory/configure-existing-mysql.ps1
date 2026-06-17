param(
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$DatabaseName = "stockwise_inventory",
    [string]$Username = "root",
    [SecureString]$Password
)

$ErrorActionPreference = "Stop"

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

function Find-MySqlClient {
    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Workbench 8.0\mysql.exe"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    return $null
}

function Test-MySqlCredentials {
    param(
        [string]$MySqlClient,
        [string]$HostName,
        [int]$Port,
        [string]$Username,
        [string]$PlainPassword
    )

    if (-not $MySqlClient) {
        Write-Host "MySQL command-line client was not found, so credentials could not be pre-checked." -ForegroundColor Yellow
        return
    }

    $defaultsFile = Join-Path ([System.IO.Path]::GetTempPath()) ("smart-invent-mysql-{0}.cnf" -f ([Guid]::NewGuid()))
    try {
        $content = @(
            "[client]",
            "user=$Username",
            "password=$PlainPassword",
            "host=$HostName",
            "port=$Port"
        )
        $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
        [System.IO.File]::WriteAllLines($defaultsFile, [string[]]$content, $utf8NoBom)

        $output = & $MySqlClient "--defaults-extra-file=$defaultsFile" "--protocol=tcp" "-N" "-B" "-e" "SELECT 1;" 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL rejected these credentials for '$Username'@$HostName on port $Port. Nothing was saved. Error: $output"
        }
    }
    finally {
        Remove-Item -LiteralPath $defaultsFile -Force -ErrorAction SilentlyContinue
    }
}

if (-not $Password) {
    $Password = Read-Host "Enter MySQL password for '$Username'@$HostName" -AsSecureString
}

$envPath = Join-Path $PSScriptRoot ".env"
$settings = [ordered]@{}

if (Test-Path -LiteralPath $envPath) {
    Get-Content -LiteralPath $envPath | ForEach-Object {
        $line = $_.Trim().Trim([char]0xFEFF)
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $separatorIndex = $line.IndexOf("=")
        $name = $line.Substring(0, $separatorIndex).Trim().Trim([char]0xFEFF)
        $value = $line.Substring($separatorIndex + 1).Trim()
        $settings[$name] = $value
    }
}

$plainPassword = ConvertFrom-SecureStringPlainText -SecureValue $Password
$jdbcUrl = "jdbc:mysql://${HostName}:${Port}/${DatabaseName}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

Test-MySqlCredentials -MySqlClient (Find-MySqlClient) -HostName $HostName -Port $Port -Username $Username -PlainPassword $plainPassword

$settings["DB_URL"] = $jdbcUrl
$settings["DB_USERNAME"] = $Username
$settings["DB_PASSWORD"] = $plainPassword
$settings["MYSQLHOST"] = $HostName
$settings["MYSQLPORT"] = "$Port"
$settings["MYSQLDATABASE"] = $DatabaseName
$settings["MYSQLUSER"] = $Username
$settings["MYSQLPASSWORD"] = $plainPassword
$settings["JPA_DDL_AUTO"] = "update"

$content = foreach ($entry in $settings.GetEnumerator()) {
    "$($entry.Key)=$($entry.Value)"
}

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllLines($envPath, [string[]]$content, $utf8NoBom)

Write-Host "Existing MySQL settings saved to $envPath" -ForegroundColor Green
Write-Host "Database: $DatabaseName"
Write-Host "Username: $Username"
Write-Host "Restart Spring Boot with the mysql profile after saving these settings." -ForegroundColor Green
