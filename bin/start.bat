@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM 颜色定义 (Windows 10/11 支持 ANSI 颜色代码)
set "GREEN=[92m"
set "YELLOW=[93m"
set "RED=[91m"
set "BLUE=[94m"
set "NC=[0m"

REM 获取项目根目录的绝对路径 (向上一级，因为脚本在bin目录)
set "PROJECT_ROOT=%~dp0.."

echo %BLUE%
echo    ▄▄▄        ▄████  ▓█████  ███▄    █ ▄▄▄█████▓▒██   ██▒
echo   ▒████▄     ██▒ ▀█▒ ▓█   ▀  ██ ▀█   █ ▓  ██▒ ▓▒▒▒ █ █ ▒░
echo   ▒██  ▀█▄  ▒██░▄▄▄░ ▒███   ▓██  ▀█ ██▒▒ ▓██░ ▒░░░  █   ░
echo   ░██▄▄▄▄██ ░▓█  ██▓ ▒▓█  ▄ ▓██▒  ▐▌██▒░ ▓██▓ ░  ░ █ █ ▒ 
echo    ▓█   ▓██▒░▒▓███▀▒ ░▒████▒▒██░   ▓██░  ▒██▒ ░ ▒██▒ ▒██▒
echo    ▒▒   ▓▒█░ ░▒   ▒  ░░ ▒░ ░░ ▒░   ▒ ▒   ▒ ░░   ▒▒ ░ ░▓ ░ %NC%
echo %GREEN%              智能AI助手平台 - 一键启动脚本%NC%
echo %BLUE%========================================================%NC%
echo.
echo %GREEN%项目根目录: %PROJECT_ROOT%%NC%
echo.
echo %YELLOW%包含的服务:%NC%
echo   - PostgreSQL 数据库 (端口: 5432)
echo   - API Premium Gateway (端口: 8081)
echo   - AgentX 后端服务 (端口: 8080)
echo   - AgentX 前端服务 (端口: 3000)
echo.

REM 检查 Docker 是否已安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo %RED%错误: Docker 未安装，请先安装 Docker%NC%
    pause
    exit /b 1
)

REM 检查 Docker Compose 是否已安装
docker-compose --version >nul 2>&1
if errorlevel 1 (
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo %RED%错误: Docker Compose 未安装，请先安装 Docker Compose%NC%
        pause
        exit /b 1
    )
    set "COMPOSE_CMD=docker compose"
) else (
    set "COMPOSE_CMD=docker-compose"
)

REM 切换到项目根目录
cd /d "%PROJECT_ROOT%"

REM 检查必要的文件是否存在
if not exist "docker-compose.yml" (
    echo %RED%错误: docker-compose.yml 文件不存在%NC%
    pause
    exit /b 1
)

if not exist "docs\sql\01_init.sql" (
    echo %RED%错误: 数据库初始化文件 'docs\sql\01_init.sql' 不存在%NC%
    pause
    exit /b 1
)

REM 检查数据库是否已存在
set "DB_EXISTS=false"
docker volume ls | findstr "agentx-postgres-data" >nul 2>&1
if not errorlevel 1 (
    set "DB_EXISTS=true"
)

REM 数据库初始化确认
if "%DB_EXISTS%"=="true" (
    echo %YELLOW%检测到已存在的数据库数据%NC%
    echo %YELLOW%是否重新初始化数据库？这将删除所有现有数据。%NC%
    echo %RED%注意: 选择 'y' 将清空所有数据库数据！%NC%
    set /p "choice=重新初始化数据库? [y/N] (默认: N): "
    
    if /i "!choice!"=="y" (
        echo %YELLOW%正在停止并删除现有容器和数据卷...%NC%
        
        REM 停止并删除容器
        %COMPOSE_CMD% down -v --remove-orphans
        
        REM 删除数据卷
        docker volume rm agentx-postgres-data >nul 2>&1
        
        echo %GREEN%数据库将被重新初始化%NC%
    ) else (
        echo %GREEN%跳过数据库初始化，使用现有数据%NC%
    )
) else (
    echo %GREEN%首次启动，将自动初始化数据库%NC%
)

echo.
echo %BLUE%开始构建和启动服务...%NC%

REM 创建日志目录
if not exist "logs" mkdir logs
if not exist "logs\backend" mkdir logs\backend
if not exist "logs\gateway" mkdir logs\gateway

REM 构建并启动服务
%COMPOSE_CMD% up --build -d
if errorlevel 1 (
    echo %RED%错误: 服务启动失败%NC%
    pause
    exit /b 1
)

echo.
echo %GREEN%正在等待服务启动...%NC%

REM 等待数据库启动
echo %YELLOW%等待数据库启动...%NC%
set "RETRIES=30"
:wait_db
docker exec agentx-postgres pg_isready -U postgres -d agentx >nul 2>&1
if not errorlevel 1 goto db_ready
set /a RETRIES=RETRIES-1
if !RETRIES! equ 0 (
    echo %RED%错误: 数据库启动超时%NC%
    pause
    exit /b 1
)
echo|set /p="."
timeout /t 2 /nobreak >nul
goto wait_db

:db_ready
echo %GREEN%数据库已启动%NC%

REM 等待API网关启动
echo %YELLOW%等待API网关启动...%NC%
set "RETRIES=30"
:wait_gateway
curl -f http://localhost:8081/api/health >nul 2>&1
if not errorlevel 1 goto gateway_ready
set /a RETRIES=RETRIES-1
if !RETRIES! equ 0 (
    echo %YELLOW%警告: API网关健康检查超时，但服务可能仍在启动中%NC%
    goto check_backend
)
echo|set /p="."
timeout /t 3 /nobreak >nul
goto wait_gateway

:gateway_ready
echo %GREEN%API网关已启动%NC%

:check_backend
REM 等待后端服务启动
echo %YELLOW%等待后端服务启动...%NC%
set "RETRIES=30"
:wait_backend
curl -f http://localhost:8080/api/health >nul 2>&1
if not errorlevel 1 goto backend_ready
set /a RETRIES=RETRIES-1
if !RETRIES! equ 0 (
    echo %YELLOW%警告: 后端服务健康检查超时，但服务可能仍在启动中%NC%
    goto check_frontend
)
echo|set /p="."
timeout /t 3 /nobreak >nul
goto wait_backend

:backend_ready
echo %GREEN%后端服务已启动%NC%

:check_frontend
REM 等待前端服务启动
echo %YELLOW%等待前端服务启动...%NC%
set "RETRIES=20"
:wait_frontend
curl -f http://localhost:3000 >nul 2>&1
if not errorlevel 1 goto frontend_ready
set /a RETRIES=RETRIES-1
if !RETRIES! equ 0 (
    echo %YELLOW%警告: 前端服务健康检查超时，但服务可能仍在启动中%NC%
    goto show_result
)
echo|set /p="."
timeout /t 3 /nobreak >nul
goto wait_frontend

:frontend_ready
echo %GREEN%前端服务已启动%NC%

:show_result
echo.
echo %GREEN%
echo 🎉 ========================================================= 🎉
echo                     🚀 AGENTX 启动完成! 🚀                   
echo 🎉 ========================================================= 🎉
echo %NC%
echo.
echo %BLUE%服务访问地址:%NC%
echo   - 前端应用: http://localhost:3000
echo   - 后端API: http://localhost:8080
echo   - API网关: http://localhost:8081
echo   - 数据库连接: localhost:5432
echo.
echo %YELLOW%🔐 默认登录账号:%NC%
echo ┌────────────────────────────────────────┐
echo │  管理员账号                            │
echo │  邮箱: admin@agentx.ai                 │
echo │  密码: admin123                       │
echo ├────────────────────────────────────────┤
echo │  测试账号                              │
echo │  邮箱: test@agentx.ai                  │
echo │  密码: test123                        │
echo └────────────────────────────────────────┘
echo.
echo %RED%⚠️  重要提示:%NC%
echo   - 首次启动已自动创建默认账号
echo   - 建议登录后立即修改默认密码
echo   - 生产环境请删除测试账号
echo.
echo %BLUE%数据库信息:%NC%
echo   - 主机: localhost
echo   - 端口: 5432
echo   - 数据库: agentx
echo   - 用户名: postgres
echo   - 密码: postgres
echo.
echo %BLUE%管理命令:%NC%
echo   - 查看服务状态: docker-compose ps
echo   - 停止所有服务: docker-compose down
echo   - 查看日志: docker-compose logs -f [服务名]
echo   - 重启服务: docker-compose restart [服务名]
echo.
echo %GREEN%🎉 AgentX 项目已成功启动！%NC%
echo.
pause 