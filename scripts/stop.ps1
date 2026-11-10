param()
. (Join-Path $PSScriptRoot 'runtime.ps1')
$lock = $null
$exitCode = 0
try {
    $lock = Get-LauncherLock
    $state = Read-LauncherState
    foreach ($name in @('frontend','backend')) {
        if ($state.ContainsKey($name)) {
            if (Test-ManagedProcess $state[$name]) {
                Stop-ManagedProcess $state[$name]
                Write-Host "Stopped $name."
            } else {
                Write-Host "The recorded $name process has already ended. Other processes were left running."
            }
            $state.Remove($name)
            Save-LauncherState $state
        }
    }
    Write-Host 'Project frontend and backend stopped. The shared private MySQL database remains available.'
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    $exitCode = 1
} finally {
    if ($lock) { $lock.Dispose() }
}
exit $exitCode
