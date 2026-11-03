param([switch]$IncludeIapQa, [switch]$IncludeReview, [switch]$IncludeReleaseTests, [switch]$Verify, [switch]$UnitTests, [string]$PublicConfigPath)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
if (@($IncludeIapQa,$IncludeReview,$IncludeReleaseTests).Where({ $_ }).Count -gt 1) { throw 'Build instrumentation variants in separate invocations.' }
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
    if ($IncludeReview) { $taskArguments += @(':app:assembleAmazonReview',':app:assembleAmazonReviewAndroidTest','-PdeviceTestBuildType=review') }
    if ($IncludeReleaseTests) { $taskArguments += @(':app:assembleAmazonReleaseAndroidTest','-PdeviceTestBuildType=release') }
    if ($Verify) { $taskArguments += ':app:lintAmazonRelease' }
    if ($UnitTests) { $taskArguments += ':app:testStandaloneDebugUnitTest' }
    Push-Location $taskRoot
    try { & .\gradlew.bat @taskArguments; if ($LASTEXITCODE -ne 0) { throw 'Cloudflare candidate build failed' } }
    finally { Pop-Location }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $apk = Join-Path $taskRoot 'app/build/outputs/apk/amazon/release/app-amazon-release-unsigned.apk'
    $archive = [IO.Compression.ZipFile]::OpenRead($apk)
    try {
        $asset = $archive.GetEntry('assets/AppstoreAuthenticationKey.pem')
        if (-not $asset) { throw 'Amazon SDK authentication key missing from release APK.' }
        $keyStream = $asset.Open()
        try { $packedHash = (Get-FileHash -InputStream $keyStream -Algorithm SHA256).Hash }
        finally { $keyStream.Dispose() }
        if ($packedHash -ne '9B2EDCD1C61A5D7B75A2082024344B2EF03B184F613F97B556E07F6B9A3C7ADA') {
            throw 'The packaged authentication key differs from the Possessory Appstore public key.'
        }
    } finally { $archive.Dispose() }
} finally {
    foreach ($key in $taskKeys) { [Environment]::SetEnvironmentVariable($key,$taskPrevious[$key],'Process') }
}
