@echo off
@REM 启用延迟变量扩展
setlocal enabledelayedexpansion
@REM 设置控制台编码为UTF-8
chcp 65001 > nul
@REM 设置颜色代码
set GREEN=[32m
set YELLOW=[33m
set RED=[31m
set NC=[0m

@REM 获取项目根目录的绝对路径
for %%i in ("%~dp0..") do set "PROJECT_ROOT=%%~fi"

@REM 设置目录
set "SCRIPT_DIR=%PROJECT_ROOT%\script"
set "SQL_DIR=%PROJECT_ROOT%\docs\sql"

@REM 切换到脚本目录
cd "%SCRIPT_DIR%"

echo %GREEN%AgentX PostgreSQL 数据库初始化脚本 (Docker Compose 版)%NC%
echo ===============================================
echo 数据库名称: agentx
echo 数据库用户: postgres
echo 数据库密码: postgres
echo 端口: 5432
echo SQL目录: %SQL_DIR%
echo ===============================================

@REM 检查 Docker 是否已安装
docker --version > nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo %RED%错误: Docker 未安装，请先安装 Docker%NC%
    exit /b 1
)

@REM 检查 Docker Compose 是否已安装
docker-compose --version > nul 2>&1
set COMPOSE_V1=%ERRORLEVEL%
docker compose version > nul 2>&1
set COMPOSE_V2=%ERRORLEVEL%

if %COMPOSE_V1% neq 0 (
    if %COMPOSE_V2% neq 0 (
        echo %RED%错误: Docker Compose 未安装，请先安装 Docker Compose%NC%
        exit /b 1
    )
)

@REM 检查 SQL 文件是否存在
if not exist "%SQL_DIR%\init.sql" (
    echo %RED%错误: 初始化 SQL 文件 '%SQL_DIR%\init.sql' 不存在%NC%
    exit /b 1
)

@REM 检查 docker-compose.yml 文件是否存在
if not exist "docker-compose.yml" (
    echo %RED%错误: docker-compose.yml 文件不存在%NC%
    exit /b 1
)

@REM 检查容器是否已存在
docker ps -a | findstr "agentx-postgres" > nul
if %ERRORLEVEL% equ 0 (
    echo %YELLOW%警告: 容器 'agentx-postgres' 已存在%NC%
    set /p REPLY=是否停止并删除已有容器? [y/N] 
    if /i "!REPLY!"=="y" (
        @REM 尝试使用 docker-compose 停止并删除容器
        echo 停止并删除容器...
        
        @REM 检查是否支持 docker compose 命令
        if %COMPOSE_V2% equ 0 (
            docker compose down -v
        ) else (
            docker-compose down -v
        )
        
        @REM 如果 docker-compose 失败，使用 docker 命令删除
        docker ps -a | findstr "agentx-postgres" > nul
        if %ERRORLEVEL% equ 0 (
            echo 使用 docker 命令停止容器...
            docker stop agentx-postgres > nul
            echo 删除容器...
            docker rm agentx-postgres > nul
        )
    ) else (
        echo 操作取消，如需重新运行请先手动停止容器
        pause
        exit /b 0
    )
)

echo %GREEN%启动 PostgreSQL 容器...%NC%

@REM 使用 docker compose 或 docker-compose 根据环境支持
if %COMPOSE_V2% equ 0 (
    docker compose up -d
) else (
    docker-compose up -d
)

@REM 检查容器是否成功启动
if %ERRORLEVEL% neq 0 (
    echo %RED%错误: 容器启动失败%NC%
    exit /b 1
)

echo %GREEN%等待 PostgreSQL 启动...%NC%
timeout /t 5 /nobreak > nul

@REM 确认 PostgreSQL 是否已准备好接受连接
set RETRIES=3
:RETRY_LOOP
if %RETRIES% leq 0 goto RETRY_END
docker exec agentx-postgres pg_isready -U postgres -d agentx > nul 2>&1
if %ERRORLEVEL% equ 0 goto RETRY_END
echo 等待 PostgreSQL 启动中，剩余尝试次数: %RETRIES%
set /a RETRIES-=1
timeout /t 3 /nobreak > nul
goto RETRY_LOOP
:RETRY_END

if %RETRIES% leq 0 (
    echo %RED%错误: PostgreSQL 启动超时%NC%
    exit /b 1
)

echo %GREEN%PostgreSQL 容器已成功启动！%NC%
echo 容器名称: agentx-postgres
echo 连接信息:
echo   主机: localhost
echo   端口: 5432
echo   用户: postgres
echo   密码: postgres
echo   数据库: agentx
echo   连接URL: jdbc:postgresql://localhost:5432/agentx
echo.
echo 你可以使用以下命令连接到数据库:
echo   docker exec -it agentx-postgres psql -U postgres -d agentx
echo.
echo %GREEN%数据库初始化完成！%NC%

@REM 显示可用的命令
echo %YELLOW%使用指南:%NC%
echo   启动容器: cd script ^&^& setup_with_compose.bat
echo   停止容器: cd script ^&^& docker-compose down 或 docker compose down
echo   查看容器状态: docker ps
echo   查看容器日志: docker logs agentx-postgres
pause