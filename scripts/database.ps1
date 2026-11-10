# The private MySQL instance is shared between project starts and survives stop.cmd.
$script:DatabaseHome = Join-Path $env:LOCALAPPDATA 'LinguaMate'
$script:DatabasePort = 34061

function Find-MySqlBin {
    if ($env:LINGUAMATE_MYSQL_BIN) { $candidates = @($env:LINGUAMATE_MYSQL_BIN) }
    else {
        $candidates = @(Get-ChildItem -Path (Join-Path $env:ProgramFiles 'MySQL') -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin' })
        $command = Get-Command mysqld.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) { $candidates += Split-Path $command.Source -Parent }
    }
    foreach ($path in $candidates) {
        if ((Test-Path -LiteralPath (Join-Path $path 'mysqld.exe')) -and (Test-Path -LiteralPath (Join-Path $path 'mysql.exe'))) {
            return [IO.Path]::GetFullPath($path)
        }
    }
    throw 'MySQL 8+ is required for the private database. Install MySQL Server or set DB_URL, DB_USER and DB_PASSWORD for your own database.'
}

function Set-DatabaseEnvironment {
    $explicit = $env:DB_URL -or $env:DB_USER -or $env:DB_PASSWORD
    if ($explicit) {
        if (!$env:DB_URL -or !$env:DB_USER) { throw 'External database configuration requires both DB_URL and DB_USER. Set DB_PASSWORD if the account uses a password.' }
        if (!$env:DB_URL.StartsWith('jdbc:mysql://')) { throw 'DB_URL must be a jdbc:mysql:// connection URL.' }
        return 'external'
    }
    $file = Join-Path $script:DatabaseHome 'db-env.ps1'
    if (!(Test-Path -LiteralPath $file)) {
        throw 'Private database configuration is missing. Run powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-local-db.ps1 once, or set DB_URL and DB_USER.'
    }
    try { . $file *> $null }
    catch { throw 'The local database credential could not be loaded for this Windows account. Use the account that initialized it, or configure an external database.' }
    if (!$env:DB_URL -or !$env:DB_USER -or !$env:DB_PASSWORD) { throw 'The private database configuration is incomplete.' }
    if ($env:DB_URL -notmatch '^jdbc:mysql://(127\.0\.0\.1|localhost):34061/linguamate(?:\?|$)') {
        throw 'The saved database URL does not identify the private 34061 instance. Set DB_URL and DB_USER explicitly for a different database.'
    }
    return 'private'
}

function Test-PrivateDatabase([string]$Bin) {
    $saved = $env:MYSQL_PWD
    $previousPreference = $ErrorActionPreference
    try {
        $env:MYSQL_PWD = $env:DB_PASSWORD
        $ErrorActionPreference = 'Continue'
        & (Join-Path $Bin 'mysql.exe') --no-defaults --protocol=TCP --connect-timeout=2 --host=127.0.0.1 "-P$script:DatabasePort" "-u$env:DB_USER" -Dlinguamate -N -e 'SELECT 1' *> $null
        return $LASTEXITCODE -eq 0
    } finally { $env:MYSQL_PWD = $saved; $ErrorActionPreference = $previousPreference }
}

function Start-PrivateDatabase {
    $bin = Find-MySqlBin
    $data = Join-Path $script:DatabaseHome 'mysql-data'
    if (!(Test-Path -LiteralPath (Join-Path $data 'mysql'))) {
        throw 'The private MySQL data directory is missing or incomplete. Run scripts/setup-local-db.ps1 for a new instance. Existing data will not be overwritten.'
    }
    $owner = Get-PortOwner $script:DatabasePort
    if ($owner) {
        $running = Get-CimInstance Win32_Process -Filter "ProcessId=$owner"
        if ($running.Name -ne 'mysqld.exe' -or !$running.CommandLine -or !$running.CommandLine.Contains($data)) {
            throw "Port $script:DatabasePort belongs to another process. The private database was not started."
        }
        if (!(Test-PrivateDatabase $bin)) { throw 'The private database is running but authentication failed. Check the saved Windows credentials.' }
        return $null
    }
    $initFile = Join-Path $script:DatabaseHome 'mysql-private/initial-setup.sql'
    $arguments = @('--no-defaults', ('--datadir="' + $data + '"'), '--bind-address=127.0.0.1',
        "--port=$script:DatabasePort", '--mysqlx=OFF', ('--log-error="' + (Join-Path $script:RuntimeRoot 'mysql.log') + '"'))
    if (Test-Path -LiteralPath $initFile) { $arguments += '--init-file="' + $initFile + '"' }
    $process = Start-Process -FilePath (Join-Path $bin 'mysqld.exe') -ArgumentList $arguments -WindowStyle Hidden -PassThru
    $identity = Get-ProcessIdentity $process.Id $data $script:DatabasePort
    try {
        for ($attempt = 0; $attempt -lt 60; $attempt++) {
            if (!(Test-ManagedProcess $identity)) { throw 'Private MySQL exited. Inspect .runtime/mysql.log.' }
            if (Test-PrivateDatabase $bin) {
                if (Test-Path -LiteralPath $initFile) { Remove-Item -LiteralPath $initFile -Force }
                return $identity
            }
            Start-Sleep -Seconds 1
        }
        throw 'Private MySQL did not become ready. Inspect .runtime/mysql.log.'
    } catch {
        Stop-PrivateDatabase $identity
        throw
    }
}

function Stop-PrivateDatabase($Record) {
    if (!(Test-ManagedProcess $Record)) { return }
    $file = Join-Path $script:DatabaseHome 'mysql-private/admin.xml'
    if (!(Test-Path -LiteralPath $file)) {
        Write-Warning 'MySQL was left running because its shutdown credential is unavailable.'
        return
    }
    $saved = $env:MYSQL_PWD
    $previousPreference = $ErrorActionPreference
    try {
        $credential = Import-Clixml -LiteralPath $file
        $env:MYSQL_PWD = $credential.GetNetworkCredential().Password
        $bin = Find-MySqlBin
        $ErrorActionPreference = 'Continue'
        & (Join-Path $bin 'mysqladmin.exe') --no-defaults --protocol=TCP --connect-timeout=3 --host=127.0.0.1 "-P$script:DatabasePort" "-u$($credential.UserName)" shutdown *> $null
        if ($LASTEXITCODE -ne 0) { Write-Warning 'MySQL shutdown failed. Its data has been preserved and the service may still be running.' }
    } finally { $env:MYSQL_PWD = $saved; $ErrorActionPreference = $previousPreference }
}
