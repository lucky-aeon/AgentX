@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM 颜色定义
set "GREEN=[92m"
set "YELLOW=[93m"
set "RED=[91m"
set "BLUE=[94m"
set "NC=[0m"

REM 获取项目根目录的绝对路径 (向上一级，因为脚本在bin目录)
set "PROJECT_ROOT=%~dp0.."

echo %BLUE%================================
echo       AgentX 服务停止脚本
echo ================================%NC%

REM 切换到项目根目录
cd /d "%PROJECT_ROOT%"

echo %YELLOW%正在停止 AgentX 项目的所有服务...%NC%

REM 检查 Docker Compose 命令
docker-compose --version >nul 2>&1
if errorlevel 1 (
    set "COMPOSE_CMD=docker compose"
) else (
    set "COMPOSE_CMD=docker-compose"
)

REM 检查是否有正在运行的容器
docker ps | findstr "agentx-" >nul 2>&1
if not errorlevel 1 (
    REM 使用 Docker Compose 停止服务
    %COMPOSE_CMD% down
    
    if not errorlevel 1 (
        echo %GREEN%所有服务已成功停止%NC%
    ) else (
        echo %RED%停止服务时出现错误%NC%
        pause
        exit /b 1
    )
) else (
    echo %YELLOW%没有发现正在运行的 AgentX 服务%NC%
)

echo %BLUE%服务状态检查:%NC%
docker ps --filter "name=agentx-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo %GREEN%✅ AgentX 项目已停止%NC%
pause 