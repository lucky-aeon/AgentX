@echo off
setlocal enabledelayedexpansion

:: 设置颜色代码
set "GREEN=[32m"
set "YELLOW=[33m"
set "RED=[31m"
set "BLUE=[34m"
set "NC=[0m"

:: 检查是否安装了 Docker
where docker >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo %RED%Error: Docker is not installed. Please install Docker Desktop for Windows first.%NC%
    exit /b 1
)

:: 检查是否安装了 Docker Compose
where docker-compose >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo %RED%Error: Docker Compose is not installed. Please install Docker Compose first.%NC%
    exit /b 1
)

:: 检查 docker-compose.yml 是否存在
if not exist "docker-compose.yml" (
    echo %RED%Error: docker-compose.yml not found in current directory!%NC%
    exit /b 1
)

:: 检查 SQL 目录是否存在
if not exist "..\docs\sql" (
    echo %RED%Error: SQL directory not found!%NC%
    exit /b 1
)

:: 检查 SQL 目录中是否包含 .sql 文件
dir "..\docs\sql\*.sql" >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo %RED%Error: No SQL files found in SQL directory!%NC%
    exit /b 1
)

:: 检查容器是否已存在
docker ps -a | findstr "agentx-postgres" >nul
if %ERRORLEVEL% equ 0 (
    echo %YELLOW%Warning: Container 'agentx-postgres' already exists%NC%
    set /p REPLY="Do you want to remove it? (y/N) "
    if /i "!REPLY!"=="y" (
        echo Removing existing container...
        docker rm -f agentx-postgres
    ) else (
        echo Operation cancelled
        exit /b 0
    )
)

echo %GREEN%Starting PostgreSQL container...%NC%
docker-compose up -d

if %ERRORLEVEL% neq 0 (
    echo %RED%Error: Failed to start container%NC%
    exit /b 1
)

echo %GREEN%Waiting for PostgreSQL to be ready...%NC%
:: 等待容器健康检查
set /a attempts=0
:WAIT_LOOP
if %attempts% geq 30 (
    echo %RED%Error: PostgreSQL container failed to become healthy within timeout.%NC%
    echo %YELLOW%Please check the logs with: docker logs agentx-postgres%NC%
    exit /b 1
)
docker inspect --format="{{.State.Health.Status}}" agentx-postgres | findstr "healthy" >nul
if %ERRORLEVEL% neq 0 (
    set /a attempts+=1
    timeout /t 2 /nobreak >nul
    goto WAIT_LOOP
)

echo.
echo %GREEN%PostgreSQL is now running and ready for connections!%NC%
echo %BLUE%Connection Information:%NC%
echo %YELLOW%  Host: localhost%NC%
echo %YELLOW%  Port: 5432%NC%
echo %YELLOW%  Database: agentx%NC%
echo %YELLOW%  Username: postgres%NC%
echo %YELLOW%  Password: postgres%NC%
echo.
echo %BLUE%To stop the container, run: docker-compose down%NC%

endlocal 