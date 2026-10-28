$ErrorActionPreference='Stop'
$taskAdb='C:\Users\User\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$taskWifi = (& $taskAdb shell settings get global wifi_on).Trim()
try {
    & $taskAdb shell svc wifi disable
    $taskDisconnected=$false
    for ($taskAttempt=0; $taskAttempt -lt 30; $taskAttempt++) {
        if ((& $taskAdb shell dumpsys connectivity) -match 'Active default network: none') { $taskDisconnected=$true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $taskDisconnected) { throw 'Offline state not established' }
    foreach ($taskProbe in @('cached-offline','expired-offline')) {
        & $taskAdb shell am force-stop app.possessory.iapqa
        & $taskAdb shell am instrument -w -e class app.possessory.BillingStateProbeTest -e billingProbe $taskProbe app.possessory.iapqa.test/androidx.test.runner.AndroidJUnitRunner *> "billing-$taskProbe-log.txt"
        if (-not (Select-String -Path "billing-$taskProbe-log.txt" -Pattern 'OK (1 test)' -SimpleMatch -Quiet)) { throw "Failed billing probe: $taskProbe" }
    }
} finally {
    & $taskAdb shell am force-stop app.possessory.iapqa
    if ($taskWifi -ne '0') { & $taskAdb shell svc wifi enable }
}
