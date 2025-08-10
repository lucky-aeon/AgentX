@echo off
setlocal enabledelayedexpansion

REM AgentX一键启动脚本 - Windows版本
REM 支持多种部署模式：local/production/external

REM 颜色定义 (Windows ANSI转义序列)
set GREEN=[32m
set YELLOW=[33m
set RED=[31m
set BLUE=[34m
set NC=[0m

REM 项目信息
echo %BLUE%
echo    ████  ████   █████  ██   ██ █████ ██   ██
echo   ██  ████  ██ ██      ███  ██   ██    ██ ██ 
echo   ██████ ██████ █████  ██ █ ██   ██     ███  
echo   ██  ██ ██  ██ ██     ██  ███   ██    ██ ██ 
echo   ██  ██ ██  ██ █████  ██   ██   ██   ██   ██
echo %NC%
echo %GREEN%            智能AI助手平台 - 统一部署工具%NC%
echo %BLUE%========================================================%NC%
echo.

echo %YELLOW%AgentX 开发环境启动%NC%
echo 本脚本适用于开发者进行本地开发
echo 如需生产环境部署，请参考: docs/deployment/PRODUCTION_DEPLOY.md
echo.

REM 检查Docker环境
:check_docker
where docker >nul 2>&1
if errorlevel 1 (
    echo %RED%错误: Docker未安装，请先安装Docker Desktop%NC%
    pause
    exit /b 1
)

docker compose version >nul 2>&1
if errorlevel 1 (
    echo %RED%错误: Docker Compose未安装或版本过低%NC%
    pause
    exit /b 1
)

echo %GREEN%✅ Docker环境检查通过%NC%
echo.

REM 设置开发模式配置
:set_development_mode
set MODE=dev
set ENV_FILE=.env.local.example
set PROFILE=local,dev
set DOCKERFILE_SUFFIX=.dev

echo %GREEN%🔥 启动开发模式%NC%
echo   - 内置数据库 + 消息队列
echo   - 代码热重载支持
echo   - 数据库管理工具 (Adminer)
echo   - 调试端口开放
echo.

REM 准备环境配置
:prepare_env
if not exist ".env" (
    echo %YELLOW%创建环境配置文件...%NC%
    if exist "%ENV_FILE%" (
        copy "%ENV_FILE%" ".env" >nul
        echo %GREEN%✅ 已创建 .env 文件，基于模板: %ENV_FILE%%NC%
    ) else (
        echo %RED%错误: 模板文件 %ENV_FILE% 不存在%NC%
        pause
        exit /b 1
    )
) else (
    echo %GREEN%✅ 使用现有 .env 配置文件%NC%
)
echo.

REM 启动服务
:start_services
echo %BLUE%启动AgentX服务...%NC%
echo 部署模式: %MODE%
echo Docker Compose Profile: %PROFILE%
echo.

REM 设置环境变量
set COMPOSE_PROFILES=%PROFILE%
set DOCKERFILE_SUFFIX=%DOCKERFILE_SUFFIX%

REM 启动服务 (支持多个profile)
echo %YELLOW%正在构建和启动容器...%NC%
docker compose --profile local --profile dev up -d --build

if errorlevel 1 (
    echo.
    echo %RED%❌ 服务启动失败，请检查错误信息%NC%
    echo.
    echo %YELLOW%常用排错命令:%NC%
    echo   查看详细日志: docker compose logs
    echo   查看容器状态: docker compose ps
    echo   重新构建: docker compose build --no-cache
    pause
    exit /b 1
)

echo.
echo %GREEN%🎉 AgentX启动完成！%NC%
echo.
echo %BLUE%服务访问地址:%NC%
echo   前端: http://localhost:3000
echo   后端API: http://localhost:8080
echo   API网关: http://localhost:8081

if "%MODE%"=="dev" (
    echo   数据库管理: http://localhost:8082
)

echo.
echo %BLUE%默认登录账号:%NC%
echo   管理员: admin@agentx.ai / admin123

if "%MODE%"=="local" (
    echo   测试用户: test@agentx.ai / test123
) else if "%MODE%"=="dev" (
    echo   测试用户: test@agentx.ai / test123
)

echo.
echo %YELLOW%常用命令:%NC%
echo   查看日志: docker compose logs -f
echo   停止服务: docker compose down
echo   重启服务: docker compose restart
echo   查看状态: docker compose ps