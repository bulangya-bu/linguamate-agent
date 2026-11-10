param(
    [switch]$NoOpen,
    [switch]$SkipBuild,
    [switch]$CheckOnly
)
. (Join-Path $PSScriptRoot 'runtime.ps1')
. (Join-Path $PSScriptRoot 'database.ps1')
$lock = $null
$state = @{}
$stateLoaded = $false
$started = New-Object Collections.Generic.List[string]
$newDatabase = $null
$exitCode = 0
$callerLocation = Get-Location

function Invoke-BuildCommand([string]$Executable, [string[]]$Arguments, [string]$Log) {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $Executable @Arguments *> $Log
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    if ($code -ne 0) { throw "Dependency installation or compilation failed (exit $code). Inspect $Log." }
}

try {
    $lock = Get-LauncherLock
    Set-Location -LiteralPath $script:ProjectRoot
    $config = Join-Path $script:RuntimeRoot 'config.ps1'
    if (Test-Path -LiteralPath $config) {
        try { . $config *> $null }
        catch { throw 'The local configuration could not be loaded. Check .runtime/config.ps1.' }
    }
    $tools = Get-DevelopmentTools
    $backendPort = Get-ConfiguredPort 'SERVER_PORT' 4040
    $frontendPort = Get-ConfiguredPort 'FRONTEND_PORT' 5173
    if ($backendPort -eq $frontendPort) { throw 'Backend and frontend ports must be different.' }
    $mode = Set-DatabaseEnvironment
    if ($mode -eq 'private') { Find-MySqlBin | Out-Null }
    $state = Read-LauncherState
    $stateLoaded = $true
    foreach ($name in @('backend','frontend')) {
        if ($state.ContainsKey($name) -and !(Test-ManagedProcess $state[$name])) { $state.Remove($name) }
    }
    Assert-PortAvailable $backendPort $state['backend']
    Assert-PortAvailable $frontendPort $state['frontend']
    foreach ($name in @('backend','frontend')) {
        $expectedPort = if ($name -eq 'backend') { $backendPort } else { $frontendPort }
        if ($state.ContainsKey($name) -and $state[$name].port -ne $expectedPort) {
            throw "The managed $name uses another port. Run stop.cmd before changing ports."
        }
    }
    if ($CheckOnly) {
        Write-Host "Checks passed. Database mode: $mode. Backend: $backendPort. Frontend: $frontendPort."
        return
    }
    $frontend = Join-Path $script:ProjectRoot 'frontend'
    $lockfile = Join-Path $frontend 'package-lock.json'
    if (!(Test-Path -LiteralPath $lockfile)) { throw 'frontend/package-lock.json is required for a reproducible npm ci.' }
    $dependencyHash = (Get-FileDigest $lockfile) + ':' +
        (Get-FileDigest (Join-Path $frontend 'package.json'))
    $dependencyStamp = Join-Path $script:RuntimeRoot 'frontend-dependencies.sha256'
    $dependenciesReady = (Test-Path -LiteralPath (Join-Path $frontend 'node_modules/vite/bin/vite.js')) -and
        (Test-Path -LiteralPath $dependencyStamp) -and (Get-Content -Raw -LiteralPath $dependencyStamp).Trim() -eq $dependencyHash
    if (!$dependenciesReady -and $state.ContainsKey('frontend')) {
        throw 'Frontend dependencies changed. Run stop.cmd, then start.cmd to install them.'
    }
    $sourceHash = Get-BackendFingerprint
    $jar = Join-Path $script:ProjectRoot 'target/java-big-work-0.0.1-SNAPSHOT.jar'
    $buildStamp = Join-Path $script:RuntimeRoot 'backend-source.sha256'
    $artifactStamp = Join-Path $script:RuntimeRoot 'backend-artifact.sha256'
    $buildCurrent = (Test-Path -LiteralPath $jar) -and (Test-Path -LiteralPath $buildStamp) -and
        (Test-Path -LiteralPath $artifactStamp) -and
        (Get-Content -Raw -LiteralPath $buildStamp).Trim() -eq $sourceHash -and
        (Get-Content -Raw -LiteralPath $artifactStamp).Trim() -eq (Get-FileDigest $jar)
    if (!$buildCurrent -and $state.ContainsKey('backend') -and !$SkipBuild) {
        throw 'Backend sources changed. Run stop.cmd, then start.cmd to compile and apply them.'
    }
    if (!$dependenciesReady) {
        Write-Host 'Installing frontend dependencies...'
        Set-Location -LiteralPath $frontend
        Invoke-BuildCommand $tools.Npm @('ci','--no-audit','--no-fund') (Join-Path $script:RuntimeRoot 'npm-install.log')
        [IO.File]::WriteAllText($dependencyStamp, $dependencyHash)
        Set-Location -LiteralPath $script:ProjectRoot
    }
    if (!$buildCurrent -and !$SkipBuild) {
        Write-Host 'Compiling the backend...'
        Invoke-BuildCommand (Join-Path $script:ProjectRoot 'mvnw.cmd') @('-B','-DskipTests','package') (Join-Path $script:RuntimeRoot 'backend-build.log')
        if (!(Test-Path -LiteralPath $jar)) { throw 'Maven completed without producing the expected backend JAR.' }
        if ((Get-BackendFingerprint) -ne $sourceHash) { throw 'Backend sources changed during compilation. Run start.cmd again after saving your edits.' }
        [IO.File]::WriteAllText($buildStamp, $sourceHash)
        [IO.File]::WriteAllText($artifactStamp, (Get-FileDigest $jar))
        $buildCurrent = $true
    }
    if (!(Test-Path -LiteralPath $jar)) { throw 'The backend JAR is missing. Run start.cmd without -SkipBuild.' }
    if ($mode -eq 'private') {
        Write-Host 'Checking the private database...'
        $newDatabase = Start-PrivateDatabase
    }
    $env:SERVER_PORT = [string]$backendPort
    $env:FRONTEND_PORT = [string]$frontendPort
    $env:FRONTEND_HOST = '127.0.0.1'
    $env:VITE_API_PROXY_TARGET = "http://127.0.0.1:$backendPort"
    if (!$env:LINGUAMATE_WEB_ALLOWED_ORIGINS) {
        $env:LINGUAMATE_WEB_ALLOWED_ORIGINS = "http://127.0.0.1:$frontendPort,http://localhost:$frontendPort"
    }
    if (!$env:LINGUAMATE_REDIS_CACHE_ENABLED) { $env:LINGUAMATE_REDIS_CACHE_ENABLED = 'false' }
    if (!$state.ContainsKey('backend')) {
        Assert-PortAvailable $backendPort $null
        $marker = 'linguamate.run.id=' + [guid]::NewGuid().ToString('N')
        $process = Start-Process -FilePath $tools.Java -ArgumentList @("-D$marker",'-jar',('"' + $jar + '"'),'--server.address=127.0.0.1') -WorkingDirectory $script:ProjectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $script:RuntimeRoot 'backend.log') -RedirectStandardError (Join-Path $script:RuntimeRoot 'backend-error.log')
        $state['backend'] = Get-ProcessIdentity $process.Id $marker $backendPort
        if ($buildCurrent) { $state['backend'] | Add-Member -NotePropertyName sourceHash -NotePropertyValue $sourceHash }
        $started.Add('backend')
        Save-LauncherState $state
    }
    Write-Host 'Waiting for backend readiness...'
    Wait-ServiceReady $state['backend'] 'backend' 180
    if (!$state.ContainsKey('frontend')) {
        Assert-PortAvailable $frontendPort $null
        $marker = 'linguamate-' + [guid]::NewGuid().ToString('N')
        $vite = Join-Path $frontend 'node_modules/vite/bin/vite.js'
        $process = Start-Process -FilePath $tools.Node -ArgumentList @("--title=$marker",('"' + $vite + '"'),'--host','127.0.0.1','--port',[string]$frontendPort,'--strictPort') -WorkingDirectory $frontend -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $script:RuntimeRoot 'frontend.log') -RedirectStandardError (Join-Path $script:RuntimeRoot 'frontend-error.log')
        $state['frontend'] = Get-ProcessIdentity $process.Id $marker $frontendPort
        $started.Add('frontend')
        Save-LauncherState $state
    }
    Wait-ServiceReady $state['frontend'] 'frontend' 60
    # Verify that the Vite proxy reaches this backend before reporting success.
    if (!(Test-FrontendProxy $frontendPort)) { throw 'The frontend API proxy is unavailable. Inspect VITE_API_PROXY_TARGET and .runtime/frontend-error.log.' }
    Save-LauncherState $state
    Write-Host "LinguaMate is ready at http://127.0.0.1:$frontendPort"
    Write-Host 'Use stop.cmd to stop this project frontend and backend. The private database stays running.'
    if (!$NoOpen) { Start-Process "http://127.0.0.1:$frontendPort" }
} catch {
    $failure = $_.Exception.Message
    for ($index = $started.Count - 1; $index -ge 0; $index--) {
        $name = $started[$index]
        try {
            Stop-ManagedProcess $state[$name]
            $state.Remove($name)
        } catch { Write-Warning "Could not roll back $name. Use stop.cmd to inspect and stop it." }
    }
    if ($newDatabase) {
        try { Stop-PrivateDatabase $newDatabase }
        catch { Write-Warning 'The private database remains running after the startup failure.' }
    }
    if ($lock -and $stateLoaded) { Save-LauncherState $state }
    Write-Host $failure -ForegroundColor Red
    $exitCode = 1
} finally {
    Set-Location -LiteralPath $callerLocation.Path
    if ($lock) { $lock.Dispose() }
}
exit $exitCode
