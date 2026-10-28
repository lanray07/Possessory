param([Parameter(Mandatory=$true)][string]$Version, [string]$Serial='G001KT0620330AWU')
$ErrorActionPreference='Stop'
if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw 'Expected semantic version.' }
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskAdb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
function Invoke-Device([string[]]$Arguments) {
    $output=& $taskAdb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($output -join "`n") }
    return $output
}
$policy=Invoke-Device @('shell','dumpsys','window','policy')
if (($policy -join "`n") -match '(?m)^\s+(showing|mIsShowing)=true') { throw 'Unlock the device before capturing.' }
$settings=@{
    stay=@('global','stay_on_while_plugged_in')
    font=@('system','font_scale')
    rotation=@('system','user_rotation')
    auto=@('system','accelerometer_rotation')
}
$original=@{}
foreach ($name in $settings.Keys) { $original[$name]=(Invoke-Device (@('shell','settings','get')+$settings[$name]) | Out-String).Trim() }
$outputRoot=Join-Path $taskRoot "artifacts/reviewer-captures-$Version"
New-Item -ItemType Directory -Force $outputRoot | Out-Null
try {
    Invoke-Device @('shell','settings','put','global','stay_on_while_plugged_in','3') | Out-Null
    Invoke-Device @('shell','settings','put','system','accelerometer_rotation','0') | Out-Null
    foreach ($set in @('standard','large-font','portrait')) {
        $font=if ($set -eq 'large-font') {'2.0'} else {'1.0'}
        $rotation=if ($set -eq 'portrait') {'0'} else {'3'}
        Invoke-Device @('shell','settings','put','system','font_scale',$font) | Out-Null
        Invoke-Device @('shell','settings','put','system','user_rotation',$rotation) | Out-Null
        $arguments=@('shell','am','instrument','-w','-e','class','app.possessory.ReleaseDeviceTest#captureAllDraftLocalesOnDevice','-e','captureSet',$set)
        if ($set -ne 'standard') { $arguments+=@('-e','locales','en-GB,de-DE,ar') }
        $arguments+='app.possessory.review.test/androidx.test.runner.AndroidJUnitRunner'
        $result=Invoke-Device $arguments
        $result | Set-Content -Encoding utf8 (Join-Path $outputRoot "$set-log.txt")
        if (($result -join "`n") -notmatch 'OK \(1 test\)') { throw "Device capture failed: $set" }
        $target=Join-Path $outputRoot $set
        if (Test-Path $target) { throw "Capture destination already exists: $target" }
        Invoke-Device @('pull',"/sdcard/Android/data/app.possessory.review/files/locale-captures-$set",$target) | Out-Null
        Write-Output "Captured $set successfully."
    }
} finally {
    foreach ($name in $settings.Keys) {
        Invoke-Device (@('shell','settings','put')+$settings[$name]+@($original[$name])) | Out-Null
    }
    Write-Output 'Restored the settings recorded before this run.'
}
