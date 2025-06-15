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
set "API_GATEWAY_DIR=%PROJECT_ROOT%\API-Premium-Gateway"

echo %BLUE%
echo    ▄▄▄        ▄████  ▓█████  ███▄    █ ▄▄▄█████▓▒██   ██▒
echo   ▒████▄     ██▒ ▀█▒ ▓█   ▀  ██ ▀█   █ ▓  ██▒ ▓▒▒▒ █ █ ▒░
echo   ▒██  ▀█▄  ▒██░▄▄▄░ ▒███   ▓██  ▀█ ██▒▒ ▓██░ ▒░░░  █   ░
echo   ░██▄▄▄▄██ ░▓█  ██▓ ▒▓█  ▄ ▓██▒  ▐▌██▒░ ▓██▓ ░  ░ █ █ ▒ 
echo    ▓█   ▓██▒░▒▓███▀▒ ░▒████▒▒██░   ▓██░  ▒██▒ ░ ▒██▒ ▒██▒
echo    ▒▒   ▓▒█░ ░▒   ▒  ░░ ▒░ ░░ ▒░   ▒ ▒   ▒ ░░   ▒▒ ░ ░▓ ░ %NC%
echo %GREEN%            智能AI助手平台 - 开发模式智能启动%NC%
echo %BLUE%========================================================%NC%
echo.
echo %GREEN%项目根目录: %PROJECT_ROOT%%NC%
echo.
echo %YELLOW%🚀 开发模式特性:%NC%
echo   - 智能依赖检查，首次自动构建
echo   - Maven/NPM 依赖缓存，加速构建
echo   - API网关自动克隆和构建
echo   - 数据库自动初始化
echo   - 服务健康检查
echo   - 🔥 支持热更新模式（文件监听+容器重启）
echo.
echo %BLUE%开发模式特性:%NC%
echo   - 文件监听: 代码变更自动重启容器
echo   - 智能检测: 自动选择最佳监听方案
echo   - 开箱即用: 无需安装额外工具
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

REM 检查 Git 是否已安装
git --version >nul 2>&1
if errorlevel 1 (
    echo %RED%错误: Git 未安装，请先安装 Git%NC%
    pause
    exit /b 1
)

REM 切换到项目根目录
cd /d "%PROJECT_ROOT%"

REM 检查必要的文件是否存在
set "COMPOSE_FILE=docker-compose.dev.yml"
echo %GREEN%🔥 开发模式（热更新已启用）%NC%

if not exist "%COMPOSE_FILE%" (
    echo %RED%错误: %COMPOSE_FILE% 文件不存在%NC%
    pause
    exit /b 1
)

if not exist "docs\sql\01_init.sql" (
    echo %RED%错误: 数据库初始化文件 'docs\sql\01_init.sql' 不存在%NC%
    pause
    exit /b 1
)

REM 检查并克隆API网关项目
echo %BLUE%1. 检查API网关项目...%NC%
if not exist "%API_GATEWAY_DIR%" (
    echo %YELLOW%API网关项目不存在，正在克隆...%NC%
    git clone https://github.com/lucky-aeon/API-Premium-Gateway.git "%API_GATEWAY_DIR%"
    if errorlevel 1 (
        echo %RED%错误: API网关项目克隆失败%NC%
        pause
        exit /b 1
    )
    echo %GREEN%✅ API网关项目克隆完成%NC%
) else (
    echo %GREEN%✅ API网关项目已存在%NC%
    REM 可选：更新API网关项目
    echo %YELLOW%正在更新API网关项目...%NC%
    cd /d "%API_GATEWAY_DIR%"
    git pull origin main >nul 2>&1 || echo %YELLOW%⚠️  API网关项目更新失败，继续使用本地版本%NC%
    cd /d "%PROJECT_ROOT%"
)

REM 检查开发镜像是否存在
echo %BLUE%2. 检查开发环境镜像...%NC%
set "NEED_BUILD=false"

docker images --format "{{.Repository}}:{{.Tag}}" | findstr "agentx-backend:dev" >nul 2>&1
if errorlevel 1 (
    echo %YELLOW%后端开发镜像不存在%NC%
    set "NEED_BUILD=true"
)

docker images --format "{{.Repository}}:{{.Tag}}" | findstr "agentx-frontend:dev" >nul 2>&1
if errorlevel 1 (
    echo %YELLOW%前端开发镜像不存在%NC%
    set "NEED_BUILD=true"
)

docker images --format "{{.Repository}}:{{.Tag}}" | findstr "agentx-api-gateway:dev" >nul 2>&1
if errorlevel 1 (
    echo %YELLOW%API网关开发镜像不存在%NC%
    set "NEED_BUILD=true"
)

REM 创建必要的缓存卷
echo %BLUE%3. 创建依赖缓存卷...%NC%
docker volume create agentx-maven-cache >nul 2>&1
docker volume create agentx-npm-cache >nul 2>&1
echo %GREEN%✅ 依赖缓存卷已就绪%NC%

REM 检查数据库是否已存在
echo %BLUE%4. 检查数据库状态...%NC%
set "DB_EXISTS=false"
docker volume ls | findstr "agentx-postgres-data" >nul 2>&1
if not errorlevel 1 (
    set "DB_EXISTS=true"
)

if "%DB_EXISTS%"=="true" (
    echo %YELLOW%检测到已存在的数据库数据%NC%
    echo %YELLOW%是否重新初始化数据库？这将删除所有现有数据。%NC%
    echo %RED%注意: 选择 'y' 将清空所有数据库数据！%NC%
    set /p "choice=重新初始化数据库? [y/N] (默认: N): "
    
    if /i "!choice!"=="y" (
        echo %YELLOW%正在重置数据库...%NC%
        
        REM 停止并删除容器
        %COMPOSE_CMD% -f "%COMPOSE_FILE%" down -v --remove-orphans
        
        REM 删除数据卷
        docker volume rm agentx-postgres-data >nul 2>&1
        
        echo %GREEN%数据库将被重新初始化%NC%
        set "NEED_BUILD=true"
    ) else (
        echo %GREEN%跳过数据库初始化，使用现有数据%NC%
    )
) else (
    echo %GREEN%首次启动，将自动初始化数据库%NC%
    set "NEED_BUILD=true"
)

REM 创建日志目录
if not exist "logs" mkdir logs
if not exist "logs\backend" mkdir logs\backend
if not exist "logs\gateway" mkdir logs\gateway
if not exist "logs\frontend" mkdir logs\frontend

echo.
echo %BLUE%5. 启动服务...%NC%

REM 根据检查结果选择启动方式
if "%NEED_BUILD%"=="true" (
    echo %YELLOW%首次启动或需要重新构建，正在构建镜像...%NC%
    echo %YELLOW%⏳ 这可能需要几分钟时间，请耐心等待...%NC%
    
    REM 构建并启动服务
    %COMPOSE_CMD% -f "%COMPOSE_FILE%" up --build -d
) else (
    echo %GREEN%使用已有镜像快速启动...%NC%
    
    REM 直接启动服务
    %COMPOSE_CMD% -f "%COMPOSE_FILE%" up -d
)

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
echo               🚀 AGENTX 开发环境启动完成! 🚀                 
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
echo %BLUE%🔧 开发模式特性:%NC%
echo   - 🔥 热更新: 代码变更自动重启容器
echo   - 📦 依赖缓存: Maven/NPM 依赖持久化
echo   - 🚀 快速启动: 增量构建，只在必要时重新编译
echo   - 🔍 健康检查: 自动检测服务状态
echo.
echo %GREEN%🎉 AgentX 开发环境已成功启动！%NC%
echo.
echo.

REM 开发模式，启动文件监听
echo %BLUE%🔍 正在启动开发模式热更新...%NC%

REM 检测 Docker Compose Watch 支持
docker compose version >nul 2>&1
if not errorlevel 1 (
    REM 检查 Docker Compose 版本是否支持 watch
    for /f "tokens=3" %%i in ('docker compose version 2^>nul ^| findstr "version"') do set "COMPOSE_VERSION=%%i"
    REM 简化版本检查，假设新版本都支持 watch
    set "WATCH_METHOD=compose-watch"
) else (
    set "WATCH_METHOD=polling"
)

echo %YELLOW%🔧 热更新配置:%NC%
if "%WATCH_METHOD%"=="compose-watch" (
    echo   - 监听方式: Docker Compose Watch (推荐)
    echo   - 性能: 高效文件监听
    echo   - 兼容性: Docker Compose v2.22.0+
) else (
    echo   - 监听方式: 轮询模式 (兼容模式)
    echo   - 性能: 定期检查文件变化
    echo   - 兼容性: 所有版本
)
echo.

echo %YELLOW%是否启动文件监听？%NC%
echo %BLUE%选择 'y' 将监听代码变更并自动重启容器%NC%
echo %BLUE%选择 'n' 将跳过监听，可手动重启服务%NC%
set /p "choice=启动文件监听? [Y/n] (默认: Y): "

if /i "%choice%"=="n" (
    echo %YELLOW%⚠️  跳过文件监听，可稍后手动重启服务%NC%
    echo %BLUE%手动重启命令:%NC%
    echo   - 重启后端: %COMPOSE_CMD% -f %COMPOSE_FILE% restart agentx-backend
    echo   - 重启前端: %COMPOSE_CMD% -f %COMPOSE_FILE% restart agentx-frontend
    echo   - 重启网关: %COMPOSE_CMD% -f %COMPOSE_FILE% restart api-gateway
    echo.
    echo %GREEN%✅ 开发环境已启动，服务正在运行中%NC%
    pause
    exit /b 0
)

echo %GREEN%🚀 正在启动文件监听...%NC%
echo.

if "%WATCH_METHOD%"=="compose-watch" (
    REM 使用 Docker Compose Watch 功能
    echo %GREEN%🚀 启动 Docker Compose Watch...%NC%
    echo %BLUE%💡 从现在开始，修改代码会自动重启对应容器%NC%
    echo %YELLOW%注意: 按 Ctrl+C 可停止监听并返回命令行%NC%
    echo.
    
    REM 直接使用现有的 watch 配置，但不重新构建已运行的服务
    %COMPOSE_CMD% -f "%COMPOSE_FILE%" -f docker-compose.watch.yml watch --no-up
    
) else (
    REM 使用轮询模式 - Windows 兼容版本
    echo %GREEN%🚀 启动轮询监听 (每5秒检查一次)...%NC%
    echo %BLUE%💡 从现在开始，修改代码会自动重启对应容器%NC%
    echo %YELLOW%注意: 按 Ctrl+C 可停止监听并返回命令行%NC%
    echo.
    
    REM 创建临时的监听脚本
    echo @echo off > temp_watch.bat
    echo setlocal enabledelayedexpansion >> temp_watch.bat
    echo set "LAST_CHECK=" >> temp_watch.bat
    echo :watch_loop >> temp_watch.bat
    echo REM 检查后端文件变化 >> temp_watch.bat
    echo for /r "AgentX\src" %%f in (*.java *.xml *.properties *.yml *.yaml) do ( >> temp_watch.bat
    echo     for %%a in ("%%f") do set "CURRENT_TIME=%%~ta" >> temp_watch.bat
    echo     if not "!CURRENT_TIME!"=="!LAST_CHECK_%%f!" ( >> temp_watch.bat
    echo         echo %YELLOW%📁 检测到后端文件变化: %%f%NC% >> temp_watch.bat
    echo         echo %BLUE%🔄 重启后端服务...%NC% >> temp_watch.bat
    echo         %COMPOSE_CMD% -f "%COMPOSE_FILE%" restart agentx-backend >> temp_watch.bat
    echo         echo %GREEN%✅ 后端服务已重启%NC% >> temp_watch.bat
    echo         set "LAST_CHECK_%%f=!CURRENT_TIME!" >> temp_watch.bat
    echo         timeout /t 10 /nobreak ^>nul >> temp_watch.bat
    echo     ) >> temp_watch.bat
    echo ) >> temp_watch.bat
    echo REM 检查前端文件变化 >> temp_watch.bat
    echo for /r "agentx-frontend-plus" %%f in (*.js *.jsx *.ts *.tsx *.css *.scss *.json) do ( >> temp_watch.bat
    echo     for %%a in ("%%f") do set "CURRENT_TIME=%%~ta" >> temp_watch.bat
    echo     if not "!CURRENT_TIME!"=="!LAST_CHECK_%%f!" ( >> temp_watch.bat
    echo         echo %YELLOW%📁 检测到前端文件变化: %%f%NC% >> temp_watch.bat
    echo         echo %BLUE%🔄 重启前端服务...%NC% >> temp_watch.bat
    echo         %COMPOSE_CMD% -f "%COMPOSE_FILE%" restart agentx-frontend >> temp_watch.bat
    echo         echo %GREEN%✅ 前端服务已重启%NC% >> temp_watch.bat
    echo         set "LAST_CHECK_%%f=!CURRENT_TIME!" >> temp_watch.bat
    echo         timeout /t 8 /nobreak ^>nul >> temp_watch.bat
    echo     ) >> temp_watch.bat
    echo ) >> temp_watch.bat
    echo timeout /t 5 /nobreak ^>nul >> temp_watch.bat
    echo goto watch_loop >> temp_watch.bat
    
    REM 运行监听脚本
    call temp_watch.bat
    
    REM 清理临时文件
    del temp_watch.bat >nul 2>&1
) 