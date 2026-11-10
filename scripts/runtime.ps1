# Shared Windows PowerShell 5.1 helpers. Do not start services when dot-sourced.
Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
$script:ProjectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$script:RuntimeRoot = Join-Path $script:ProjectRoot '.runtime'
$script:StateFile = Join-Path $script:RuntimeRoot 'processes.json'

function Get-LauncherLock {
    [IO.Directory]::CreateDirectory($script:RuntimeRoot) | Out-Null
    $path = Join-Path $script:RuntimeRoot 'launcher.lock'
    for ($attempt = 0; $attempt -lt 100; $attempt++) {
        try { return [IO.File]::Open($path, 'OpenOrCreate', 'ReadWrite', 'None') }
        catch [IO.IOException] { Start-Sleep -Milliseconds 200 }
    }
    throw 'Another LinguaMate start/stop command is running. Retry after it finishes.'
}

function Read-LauncherState {
    $state = @{}
    if (Test-Path -LiteralPath $script:StateFile) {
        try {
            $saved = Get-Content -Raw -Encoding UTF8 -LiteralPath $script:StateFile | ConvertFrom-Json
            foreach ($property in $saved.PSObject.Properties) { $state[$property.Name] = $property.Value }
        } catch { throw "Cannot read $script:StateFile. Keep it for inspection; no processes were stopped." }
    }
    return $state
}

function Save-LauncherState($State) {
    $temporary = Join-Path $script:RuntimeRoot 'processes.next.json'
    $json = ConvertTo-Json -InputObject $State -Depth 5
    [IO.File]::WriteAllText($temporary, $json, (New-Object Text.UTF8Encoding($false)))
    Move-Item -LiteralPath $temporary -Destination $script:StateFile -Force
}

function Get-PortOwner([int]$Port) {
    $owners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)
    if ($owners.Count -gt 1) { throw "Port $Port has multiple listeners. Resolve the port conflict first." }
    if ($owners.Count -eq 1) { return [int]$owners[0] }
    return $null
}

function Get-ProcessIdentity([int]$ProcessId, [string]$Marker, [int]$Port = 0) {
    $process = $null
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$ProcessId" -ErrorAction SilentlyContinue
        if ($process -and $process.CommandLine) { break }
        Start-Sleep -Milliseconds 100
    }
    if (!$process -or !$process.CommandLine -or !$process.CommandLine.Contains($Marker)) {
        throw 'The new service could not be identified.'
    }
    return [pscustomobject]@{
        id = $ProcessId
        created = $process.CreationDate.ToUniversalTime().ToString('o')
        executable = $process.ExecutablePath
        marker = $Marker
        port = $Port
        project = $script:ProjectRoot
    }
}

function Test-ManagedProcess($Record) {
    if (!$Record) { return $false }
    foreach ($name in @('id','executable','created','marker','project')) {
        if (!$Record.PSObject.Properties[$name]) { return $false }
    }
    if ($Record.project -ne $script:ProjectRoot -or !$Record.marker) { return $false }
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($Record.id)" -ErrorAction SilentlyContinue
    return [bool]($process -and $process.CommandLine -and
        $process.ExecutablePath -eq $Record.executable -and
        $process.CreationDate.ToUniversalTime().ToString('o') -eq $Record.created -and
        $process.CommandLine.Contains($Record.marker))
}

function Test-FrontendProxy([int]$Port) {
    try {
        $response = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/auth/me" -TimeoutSec 3
        return $response.success -eq $true
    } catch { return $false }
}

function Stop-ManagedProcess($Record) {
    if (!(Test-ManagedProcess $Record)) { return }
    Stop-Process -Id $Record.id -ErrorAction Stop
    Wait-Process -Id $Record.id -Timeout 15 -ErrorAction SilentlyContinue
}

function Assert-PortAvailable([int]$Port, $Record) {
    $owner = Get-PortOwner $Port
    if ($null -ne $owner -and (!(Test-ManagedProcess $Record) -or $owner -ne $Record.id)) {
        throw "Port $Port is used by an unmanaged process (PID $owner). Stop that application explicitly or select another port."
    }
}

function Get-ConfiguredPort([string]$Name, [int]$Default) {
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) { return $Default }
    $port = 0
    if (![int]::TryParse($value, [ref]$port) -or $port -lt 1024 -or $port -gt 65535) {
        throw "$Name must be an integer between 1024 and 65535."
    }
    return $port
}

function Find-Tool([string]$Name) {
    $command = Get-Command $Name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$command) { throw "$Name was not found. Install the required runtime and reopen the terminal." }
    return $command.Source
}

function Get-DevelopmentTools {
    $java = Find-Tool 'java.exe'
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
        $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
    }
    # Java writes its version to stderr. A temporary file avoids PS5 treating that as a terminating error.
    $versionLog = Join-Path $script:RuntimeRoot 'java-version.txt'
    $probe = Start-Process -FilePath $java -ArgumentList '-XshowSettings:properties','-version' -WindowStyle Hidden -Wait -PassThru -RedirectStandardError $versionLog
    $versionText = Get-Content -Raw -LiteralPath $versionLog
    if ($probe.ExitCode -ne 0 -or $versionText -notmatch 'java.version\s*=\s*(\d+)') {
        throw 'Cannot determine Java version. Install a JDK 17 or newer.'
    }
    if ([int]$Matches[1] -lt 17) { throw 'JDK 17 or newer is required.' }
    if ($versionText -notmatch 'java.home\s*=\s*([^\r\n]+)') { throw 'Cannot determine JAVA_HOME.' }
    $env:JAVA_HOME = $Matches[1].Trim()
    if (!(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
        throw 'A full JDK is required. This Java installation has no javac.exe.'
    }
    $node = Find-Tool 'node.exe'
    $npm = Find-Tool 'npm.cmd'
    $nodeText = & $node --version
    if ($LASTEXITCODE -ne 0 -or $nodeText -notmatch '^v(\d+)\.(\d+)\.(\d+)') { throw 'Cannot determine Node.js version.' }
    $major = [int]$Matches[1]; $minor = [int]$Matches[2]
    if (!(($major -eq 20 -and $minor -ge 19) -or ($major -eq 22 -and $minor -ge 12) -or $major -gt 22)) {
        throw 'Node.js 20.19+, 22.12+, or a newer supported release is required.'
    }
    return @{ Java = (Join-Path $env:JAVA_HOME 'bin/java.exe'); Node = $node; Npm = $npm }
}

function Get-FileDigest([string]$Path) {
    $stream = [IO.File]::OpenRead($Path)
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','') }
    finally { $stream.Dispose(); $sha.Dispose() }
}

function Get-BackendFingerprint {
    $inputs = @((Join-Path $script:ProjectRoot 'pom.xml'))
    $inputs += @(Get-ChildItem -LiteralPath (Join-Path $script:ProjectRoot 'src/main') -File -Recurse | ForEach-Object FullName)
    $hashes = @($inputs | Sort-Object | ForEach-Object {
        $_.Substring($script:ProjectRoot.Length) + ':' + (Get-FileDigest $_)
    })
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($hashes -join "`n")))).Replace('-','') }
    finally { $sha.Dispose() }
}

function Test-BackendReady([int]$Port) {
    try {
        $response = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/actuator/health/readiness" -TimeoutSec 3
        return $response.status -eq 'UP' -and $response.components.db.status -eq 'UP' -and $response.components.readinessState.status -eq 'UP'
    } catch { return $false }
}

function Test-FrontendReady([int]$Port) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/" -UseBasicParsing -TimeoutSec 3
        return $response.StatusCode -eq 200 -and $response.Content.Contains('/@vite/client')
    } catch { return $false }
}

function Wait-ServiceReady($Record, [string]$Kind, [int]$Seconds = 120) {
    $deadline = [DateTime]::UtcNow.AddSeconds($Seconds)
    do {
        if (!(Test-ManagedProcess $Record)) { throw "$Kind exited. Inspect .runtime/$Kind-error.log and .runtime/$Kind.log." }
        $ready = if ($Kind -eq 'backend') { Test-BackendReady $Record.port } else { Test-FrontendReady $Record.port }
        if ($ready -and (Get-PortOwner $Record.port) -eq $Record.id) { return }
        Start-Sleep -Seconds 1
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "$Kind did not become ready. Inspect .runtime/$Kind-error.log and .runtime/$Kind.log."
}
