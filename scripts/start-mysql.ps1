# 启动监听 3307 的 MySQL 9.5 实例，等端口真正就绪后才返回
#
# 用法
#   powershell -ExecutionPolicy Bypass -File scripts\start-mysql.ps1
#
# 退出码
#   0  已在监听，或本次启动成功
#   1  启动失败或等待超时
#
# 启动 Windows 服务需要管理员权限，检测到权限不足时脚本会把自己重新拉起一次
# 提权后的窗口看不到输出，所以结果由未提权的那一轮负责打印

[CmdletBinding()]
param(
    [switch]$Elevated
)

$ErrorActionPreference = 'Stop'

$ServiceName    = 'MySQL95'
$Port           = 3307
$ExpectProcess  = 'mysqld'
$TimeoutSeconds = 60
$ReadyTimeout   = 500

# 端口是否被 mysqld 占着
# 只看端口不认进程的话，别的程序占了也会被判成已经起好
function Test-OurMysql {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $conn) { return $false }
    $proc = Get-Process -Id $conn[0].OwningProcess -ErrorAction SilentlyContinue
    return [bool]($proc -and $proc.ProcessName -like "*$ExpectProcess*")
}

function Test-Admin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Wait-OurMysql {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-OurMysql) { return $true }
        Start-Sleep -Milliseconds $ReadyTimeout
    }
    return $false
}

# 失败时把现成的线索一次性打出来，省得再手工敲一遍
function Show-Diagnosis {
    $svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
    Write-Host "        服务状态 $(if ($svc) { $svc.Status } else { '服务不存在' })"

    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($conn) {
        $proc = Get-Process -Id $conn[0].OwningProcess -ErrorAction SilentlyContinue
        Write-Host "        占用 $Port 的进程 $($proc.ProcessName) PID $($conn[0].OwningProcess)"
    } else {
        Write-Host "        占用 $Port 的进程 无"
    }

    Write-Host "        手动启动看报错"
    Write-Host "          sc start $ServiceName"
}

if (Test-OurMysql) {
    Write-Host "[OK] 端口 $Port 已在监听，无需启动"
    exit 0
}

$svc = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if (-not $svc) {
    Write-Host "[FAIL] 找不到名为 $ServiceName 的服务"
    Write-Host "        用 Get-Service *mysql* 看看实际叫什么"
    exit 1
}

if (-not $Elevated -and -not (Test-Admin)) {
    Write-Host "[..] 启动服务需要管理员权限，正在请求提权"
    try {
        $argList = '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"", '-Elevated'
        Start-Process powershell -Verb RunAs -Wait -ArgumentList $argList
    } catch {
        Write-Host "[FAIL] 未取得管理员权限，脚本终止"
        exit 1
    }
} else {
    Write-Host "[..] 服务 $ServiceName 当前状态 $($svc.Status)"
    if ($svc.Status -eq 'Running') {
        Write-Host "[..] 服务已在运行，只等端口就绪"
    } else {
        Write-Host "[..] 正在启动服务"
        Start-Service -Name $ServiceName
    }
}

if (Wait-OurMysql) {
    Write-Host "[OK] MySQL 已启动，端口 $Port 正在监听"
    exit 0
}

Write-Host "[FAIL] 等待 $TimeoutSeconds 秒后端口 $Port 仍未就绪"
Show-Diagnosis
exit 1
