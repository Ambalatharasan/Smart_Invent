param(
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [SecureString]$AppPassword
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

if (-not $AppPassword) {
    $AppPassword = Read-Host "Enter Gmail App Password for $Email" -AsSecureString
}

$plainPassword = (ConvertFrom-SecureStringPlainText -SecureValue $AppPassword) -replace "\s+", ""
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

$settings["MAIL_ENABLED"] = "true"
$settings["MAIL_FROM"] = $Email
$settings["SPRING_MAIL_HOST"] = "smtp.gmail.com"
$settings["SPRING_MAIL_PORT"] = "587"
$settings["SPRING_MAIL_USERNAME"] = $Email
$settings["SPRING_MAIL_PASSWORD"] = $plainPassword
$settings["SPRING_MAIL_SMTP_AUTH"] = "true"
$settings["SPRING_MAIL_SMTP_STARTTLS_ENABLE"] = "true"

$content = foreach ($entry in $settings.GetEnumerator()) {
    "$($entry.Key)=$($entry.Value)"
}

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllLines($envPath, [string[]]$content, $utf8NoBom)

Write-Host "Mail settings saved to $envPath" -ForegroundColor Green
Write-Host "Restart Spring Boot, then register a new user to send a verification email." -ForegroundColor Green
