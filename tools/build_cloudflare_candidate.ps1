param([switch]$IncludeIapQa, [switch]$Verify, [string]$PublicConfigPath)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskConfigPath = if ($PublicConfigPath) { $PublicConfigPath } else { Join-Path $taskRoot 'server\cloudflare\android-public-config.json' }
$taskConfig = Get-Content -LiteralPath $taskConfigPath -Raw | ConvertFrom-Json
$taskKeys = @('POSSESSORY_VERIFIER_URL','POSSESSORY_LICENSE_PUBLIC_KEY','POSSESSORY_SANDBOX_VERIFIER_URL','POSSESSORY_SANDBOX_LICENSE_PUBLIC_KEY')
$taskPrevious = @{}
foreach ($key in $taskKeys) { $taskPrevious[$key] = [Environment]::GetEnvironmentVariable($key,'Process') }
try {
    $env:POSSESSORY_VERIFIER_URL = $taskConfig.production.verifierUrl
    $env:POSSESSORY_LICENSE_PUBLIC_KEY = $taskConfig.production.publicKey
    $env:POSSESSORY_SANDBOX_VERIFIER_URL = $taskConfig.sandbox.verifierUrl
    $env:POSSESSORY_SANDBOX_LICENSE_PUBLIC_KEY = $taskConfig.sandbox.publicKey
    $taskArguments = @(':app:assembleAmazonRelease','--max-workers=2','--console=plain')
    if ($IncludeIapQa) { $taskArguments += @(':app:assembleAmazonIapqa',':app:assembleAmazonIapqaAndroidTest','-PdeviceTestBuildType=iapqa') }
    if ($Verify) { $taskArguments += ':app:lintAmazonRelease' }
    Push-Location $taskRoot
    try { & .\gradlew.bat @taskArguments; if ($LASTEXITCODE -ne 0) { throw 'Cloudflare candidate build failed' } }
    finally { Pop-Location }
} finally {
    foreach ($key in $taskKeys) { [Environment]::SetEnvironmentVariable($key,$taskPrevious[$key],'Process') }
}
