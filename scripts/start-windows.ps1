param(
    [string]$MySqlHost = "127.0.0.1",
    [int]$MySqlPort = 3306,
    [string]$MySqlUser = "root",
    [string]$MySqlPassword = "root",
    [switch]$SkipDbInit,
    [switch]$SkipNpmInstall
)

$ErrorActionPreference = "Stop"

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackendDir = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend"
$DbInitDir = Join-Path $RootDir "deploy\mysql\init"
$LogDir = Join-Path $RootDir "tmp\windows-logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "OK  $Message" -ForegroundColor Green
}

function Write-FailAndExit {
    param([string]$Message)
    Write-Host "ERROR  $Message" -ForegroundColor Red
    exit 1
}

function Require-Command {
    param(
        [string]$Name,
        [string]$InstallHint
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        Write-FailAndExit "$Name was not found. $InstallHint"
    }
    return $command.Source
}

function Get-MajorVersion {
    param([string]$Text)
    if ($Text -match "(\d+)(\.\d+)?(\.\d+)?") {
        return [int]$Matches[1]
    }
    return 0
}

function Test-PortInUse {
    param(
        [int]$Port,
        [string]$HostName = "127.0.0.1"
    )
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $client.Connect($HostName, $Port)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Invoke-Mysql {
    param(
        [string[]]$ExtraArgs = @(),
        [string]$InputSql = $null
    )

    $mysqlArgs = @("-h", $MySqlHost, "-P", [string]$MySqlPort, "-u", $MySqlUser)
    if ($MySqlPassword -ne "") {
        $mysqlArgs += "-p$MySqlPassword"
    }
    $mysqlArgs += $ExtraArgs
    if ($null -ne $InputSql) {
        $InputSql | & mysql @mysqlArgs
    } else {
        & mysql @mysqlArgs
    }
}

function Quote-PS {
    param([string]$Value)
    return "'" + ($Value -replace "'", "''") + "'"
}

function New-EnvironmentCommand {
    param([hashtable]$EnvVars)

    $parts = @()
    foreach ($key in $EnvVars.Keys) {
        $parts += '$env:' + $key + ' = ' + (Quote-PS ([string]$EnvVars[$key]))
    }
    return ($parts -join "; ")
}

function Start-LoggedProcess {
    param(
        [string]$Name,
        [string]$Command,
        [string]$WorkingDirectory,
        [hashtable]$EnvVars = @{}
    )

    $stdout = Join-Path $LogDir "$Name.out.log"
    $stderr = Join-Path $LogDir "$Name.err.log"
    $pidFile = Join-Path $LogDir "$Name.pid"
    Remove-Item -Force -ErrorAction SilentlyContinue $stdout, $stderr, $pidFile

    $envCommand = New-EnvironmentCommand $EnvVars
    $fullCommand = if ($envCommand) { "$envCommand; $Command" } else { $Command }

    $powershellCommand = Get-Command pwsh -ErrorAction SilentlyContinue
    if (-not $powershellCommand) {
        $powershellCommand = Get-Command powershell -ErrorAction Stop
    }

    $process = Start-Process `
        -FilePath $powershellCommand.Source `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $fullCommand) `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru

    Set-Content -Path $pidFile -Value $process.Id
    Write-Ok "$Name started, pid=$($process.Id)"
}

function Wait-HttpOk {
    param(
        [string]$Name,
        [string]$Url,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                Write-Ok "$Name is ready"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    Write-Host ""
    Write-Host "Last logs for ${Name}:" -ForegroundColor Yellow
    Get-ChildItem $LogDir -Filter "$Name.*.log" -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "--- $($_.Name) ---" -ForegroundColor Yellow
        Get-Content $_.FullName -Tail 40 -ErrorAction SilentlyContinue
    }
    Write-FailAndExit "$Name did not become ready in ${TimeoutSeconds}s. Check logs in $LogDir"
}

Write-Step "Checking required tools"
$javaPath = Require-Command "java" "Install JDK 17 and add java.exe to PATH."
$mvnPath = Require-Command "mvn" "Install Maven and add mvn.cmd to PATH."
$nodePath = Require-Command "node" "Install Node.js 18 or newer and add node.exe to PATH."
$npmPath = Require-Command "npm" "Install Node.js 18 or newer and add npm.cmd to PATH."
$mysqlPath = Require-Command "mysql" "Install MySQL Client and add mysql.exe to PATH."

$javaLine = (& java -version 2>&1 | Select-Object -First 1) -join ""
$javaMajor = Get-MajorVersion $javaLine
if ($javaMajor -lt 17) {
    Write-FailAndExit "Java 17 or newer is required. Current: $javaLine"
}
Write-Ok "Java: $javaLine"

$nodeLine = (& node --version) -join ""
$nodeMajor = Get-MajorVersion $nodeLine
if ($nodeMajor -lt 18) {
    Write-FailAndExit "Node.js 18 or newer is required. Current: $nodeLine"
}
Write-Ok "Node.js: $nodeLine"
Write-Ok "Maven: $mvnPath"
Write-Ok "npm: $npmPath"
Write-Ok "mysql: $mysqlPath"

Write-Step "Checking application ports"
$appPorts = @(8080, 8101, 8102, 8103, 8104, 8105, 8106, 8107, 8108, 8109, 5173, 5174)
foreach ($port in $appPorts) {
    if (Test-PortInUse $port) {
        Write-FailAndExit "Port $port is already in use. Stop the process using this port and run this script again."
    }
}
Write-Ok "Application ports are free"

Write-Step "Checking MySQL connection"
if (-not (Test-PortInUse -HostName $MySqlHost -Port $MySqlPort)) {
    Write-FailAndExit "MySQL port $MySqlPort is not listening. Please start MySQL first."
}
Invoke-Mysql -ExtraArgs @("-e", "SELECT 1;") | Out-Null
Write-Ok "MySQL is reachable at ${MySqlHost}:${MySqlPort}"

if (-not $SkipDbInit) {
    Write-Step "Initializing MySQL databases"
    Get-ChildItem $DbInitDir -Filter "*.sql" | Sort-Object Name | ForEach-Object {
        Write-Host "Running $($_.Name)"
        $sql = Get-Content $_.FullName -Raw -Encoding UTF8
        Invoke-Mysql -InputSql $sql
    }
    Write-Ok "Database init scripts completed"
}

Write-Step "Installing common backend module"
Push-Location $BackendDir
try {
    & mvn -q -pl emr-common -DskipTests install
} finally {
    Pop-Location
}
Write-Ok "emr-common installed"

Write-Step "Installing frontend dependencies when needed"
$frontendApps = @(
    @{ Name = "emr-frontend"; Port = 5173 },
    @{ Name = "manage_code"; Port = 5174 }
)
foreach ($app in $frontendApps) {
    $appDir = Join-Path $FrontendDir $app.Name
    $nodeModules = Join-Path $appDir "node_modules"
    if (-not (Test-Path $nodeModules)) {
        if ($SkipNpmInstall) {
            Write-FailAndExit "$($app.Name)\node_modules was not found and -SkipNpmInstall was set."
        }
        Write-Host "Installing dependencies for $($app.Name)"
        Push-Location $appDir
        try {
            & npm install
        } finally {
            Pop-Location
        }
    } else {
        Write-Ok "$($app.Name) dependencies exist"
    }
}

$dbUrlSuffix = "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$services = @(
    @{ Name = "emr-auth-service"; Port = 8101; Prefix = "AUTH"; Database = "emr_auth" },
    @{ Name = "emr-user-service"; Port = 8102; Prefix = "USER"; Database = "emr_user" },
    @{ Name = "emr-visit-service"; Port = 8103; Prefix = "VISIT"; Database = "emr_visit" },
    @{ Name = "emr-record-service"; Port = 8104; Prefix = "RECORD"; Database = "emr_record" },
    @{ Name = "emr-clinical-service"; Port = 8105; Prefix = "CLINICAL"; Database = "emr_clinical" },
    @{ Name = "emr-workflow-service"; Port = 8106; Prefix = "WORKFLOW"; Database = "emr_workflow"; ExtraEnv = @{
        EMR_CLINICAL_BASE_URL = "http://127.0.0.1:8105";
        EMR_RECORD_BASE_URL = "http://127.0.0.1:8104"
    } },
    @{ Name = "emr-billing-service"; Port = 8107; Prefix = "BILLING"; Database = "emr_billing" },
    @{ Name = "emr-system-service"; Port = 8108; Prefix = "SYSTEM"; Database = "emr_system" },
    @{ Name = "emr-ai-service"; Port = 8109; Prefix = "AI"; Database = "emr_ai"; ExtraEnv = @{
        EMR_RECORD_BASE_URL = "http://127.0.0.1:8104";
        EMR_AI_PROVIDER = "mock"
    } }
)

Write-Step "Starting backend services"
foreach ($service in $services) {
    $envVars = @{}
    $envVars["EMR_" + $service.Prefix + "_DB_URL"] = "jdbc:mysql://${MySqlHost}:$MySqlPort/$($service.Database)$dbUrlSuffix"
    $envVars["EMR_" + $service.Prefix + "_DB_USERNAME"] = $MySqlUser
    $envVars["EMR_" + $service.Prefix + "_DB_PASSWORD"] = $MySqlPassword
    if ($service.ExtraEnv) {
        foreach ($key in $service.ExtraEnv.Keys) {
            $envVars[$key] = $service.ExtraEnv[$key]
        }
    }
    Start-LoggedProcess -Name $service.Name -Command "mvn -q -pl $($service.Name) spring-boot:run" -WorkingDirectory $BackendDir -EnvVars $envVars
}

$gatewayEnv = @{
    EMR_AUTH_SERVICE_URL = "http://127.0.0.1:8101";
    EMR_USER_SERVICE_URL = "http://127.0.0.1:8102";
    EMR_VISIT_SERVICE_URL = "http://127.0.0.1:8103";
    EMR_RECORD_SERVICE_URL = "http://127.0.0.1:8104";
    EMR_CLINICAL_SERVICE_URL = "http://127.0.0.1:8105";
    EMR_WORKFLOW_SERVICE_URL = "http://127.0.0.1:8106";
    EMR_BILLING_SERVICE_URL = "http://127.0.0.1:8107";
    EMR_SYSTEM_SERVICE_URL = "http://127.0.0.1:8108";
    EMR_AI_SERVICE_URL = "http://127.0.0.1:8109";
    EMR_AUTH_SERVICE_BASE_URL = "http://127.0.0.1:8101";
    EMR_AUDIT_SYSTEM_SERVICE_BASE_URL = "http://127.0.0.1:8108"
}
Start-LoggedProcess -Name "emr-gateway" -Command "mvn -q -pl emr-gateway spring-boot:run" -WorkingDirectory $BackendDir -EnvVars $gatewayEnv

Write-Step "Waiting for backend health checks"
foreach ($service in $services) {
    Wait-HttpOk -Name $service.Name -Url "http://127.0.0.1:$($service.Port)/health" -TimeoutSeconds 240
}
Wait-HttpOk -Name "emr-gateway" -Url "http://127.0.0.1:8080/health" -TimeoutSeconds 240

Write-Step "Starting frontend apps"
foreach ($app in $frontendApps) {
    $appDir = Join-Path $FrontendDir $app.Name
    Start-LoggedProcess -Name $app.Name -Command "npm run dev -- --host 0.0.0.0 --port $($app.Port)" -WorkingDirectory $appDir
}

Write-Step "Waiting for frontend pages"
Wait-HttpOk -Name "emr-frontend" -Url "http://127.0.0.1:5173/" -TimeoutSeconds 90
Wait-HttpOk -Name "manage_code" -Url "http://127.0.0.1:5174/" -TimeoutSeconds 90

Write-Host ""
Write-Host "EMR system is running." -ForegroundColor Green
Write-Host "Patient app: http://localhost:5173"
Write-Host "Admin app:   http://localhost:5174"
Write-Host "Gateway:     http://localhost:8080"
Write-Host ""
Write-Host "Demo accounts:"
Write-Host "patient_demo / 123456      role: patient"
Write-Host "admin        / admin123    role: admin"
Write-Host "doctor       / 123456      role: doctor"
Write-Host "nurse        / 123456      role: nurse"
Write-Host "director     / 123456      role: director"
Write-Host ""
Write-Host "Logs and pid files: $LogDir"
Write-Host "To stop services, run:"
Write-Host "Get-Content `"$LogDir\*.pid`" | ForEach-Object { Stop-Process -Id `$_ -Force }"
