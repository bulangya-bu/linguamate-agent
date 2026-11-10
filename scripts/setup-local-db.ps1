param()
. (Join-Path $PSScriptRoot 'runtime.ps1')
. (Join-Path $PSScriptRoot 'database.ps1')
$lock = $null
$exitCode = 0
try {
    $lock = Get-LauncherLock
    $bin = Find-MySqlBin
    $version = & (Join-Path $bin 'mysqld.exe') --version
    if ($LASTEXITCODE -ne 0 -or $version -notmatch 'Ver\s+(\d+)' -or [int]$Matches[1] -lt 8) {
        throw 'Install MySQL Server 8 or newer before initializing the private database.'
    }
    $data = Join-Path $script:DatabaseHome 'mysql-data'
    $private = Join-Path $script:DatabaseHome 'mysql-private'
    $config = Join-Path $script:DatabaseHome 'db-env.ps1'
    if (Test-Path -LiteralPath $config) {
        Write-Host 'Private database configuration already exists. Run start.cmd to use it.'
        return
    }
    if ((Test-Path -LiteralPath $data) -and @(Get-ChildItem -LiteralPath $data -Force).Count) {
        throw "Existing MySQL data was found at $data. Preserve it and restore its credential file, or configure an external database."
    }
    if (Get-PortOwner $script:DatabasePort) { throw "Port $script:DatabasePort is occupied. Resolve this before initializing MySQL." }
    [IO.Directory]::CreateDirectory($private) | Out-Null
    [IO.Directory]::CreateDirectory($data) | Out-Null
    # Restrict the short-lived initialization SQL and DPAPI credential files to this user.
    $acl = New-Object Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true, $false)
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl.SetOwner($identity)
    $rule = New-Object Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
    $acl.AddAccessRule($rule)
    Set-Acl -LiteralPath $private -AclObject $acl
    $process = Start-Process -FilePath (Join-Path $bin 'mysqld.exe') -ArgumentList '--no-defaults','--initialize-insecure',('--datadir="' + $data + '"'),'--console' -WindowStyle Hidden -Wait -PassThru -RedirectStandardOutput (Join-Path $script:RuntimeRoot 'mysql-initialize.log') -RedirectStandardError (Join-Path $script:RuntimeRoot 'mysql-initialize-error.log')
    if ($process.ExitCode -ne 0) { throw 'MySQL initialization failed. Inspect .runtime/mysql-initialize-error.log. The existing files have been retained.' }
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $bytes = New-Object byte[] 32
        $rng.GetBytes($bytes)
        $adminPassword = [Convert]::ToBase64String($bytes)
        $rng.GetBytes($bytes)
        $appPassword = [Convert]::ToBase64String($bytes)
    } finally { $rng.Dispose() }
    $admin = New-Object Management.Automation.PSCredential('root',(ConvertTo-SecureString $adminPassword -AsPlainText -Force))
    $app = New-Object Management.Automation.PSCredential('linguamate',(ConvertTo-SecureString $appPassword -AsPlainText -Force))
    $admin | Export-Clixml -LiteralPath (Join-Path $private 'admin.xml')
    $app | Export-Clixml -LiteralPath (Join-Path $private 'app.xml')
    $sql = @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$adminPassword';
CREATE DATABASE IF NOT EXISTS linguamate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'linguamate'@'localhost' IDENTIFIED BY '$appPassword';
CREATE USER IF NOT EXISTS 'linguamate'@'127.0.0.1' IDENTIFIED BY '$appPassword';
GRANT ALL PRIVILEGES ON linguamate.* TO 'linguamate'@'localhost';
GRANT ALL PRIVILEGES ON linguamate.* TO 'linguamate'@'127.0.0.1';
"@
    [IO.File]::WriteAllText((Join-Path $private 'initial-setup.sql'), $sql, (New-Object Text.UTF8Encoding($false)))
    $configuration = @'
$lmCredential = Import-Clixml -LiteralPath (Join-Path $PSScriptRoot 'mysql-private/app.xml')
$env:DB_USER = $lmCredential.UserName
$env:DB_PASSWORD = $lmCredential.GetNetworkCredential().Password
$env:DB_URL = 'jdbc:mysql://127.0.0.1:34061/linguamate?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8'
'@
    [IO.File]::WriteAllText($config, $configuration, (New-Object Text.UTF8Encoding($true)))
    . $config
    Start-PrivateDatabase | Out-Null
    Write-Host "Private database initialized on 127.0.0.1:$script:DatabasePort. Run start.cmd."
    Write-Host "Data and Windows-encrypted credentials are stored in $script:DatabaseHome."
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    $exitCode = 1
} finally {
    $adminPassword = $null; $appPassword = $null; $sql = $null
    if ($lock) { $lock.Dispose() }
}
exit $exitCode
