@echo off
setlocal enabledelayedexpansion

:: 设置颜色代码
set "GREEN=[32m"
set "YELLOW=[33m"
set "RED=[31m"
set "BLUE=[34m"
set "NC=[0m"

:: 设置变量
set "CONTAINER_NAME=agentx-postgres"
set "DB_NAME=agentx"
set "DB_USER=postgres"
set "DB_PASSWORD=postgres"
set "DB_PORT=5432"
set "HOST_PORT=5432"

:: 获取项目根目录的绝对路径
pushd %~dp0\..
set "PROJECT_ROOT=%CD%"
popd
set "SQL_DIR=%PROJECT_ROOT%\docs\sql"
set "INIT_SQL=%SQL_DIR%\init.sql"

echo %GREEN%AgentX PostgreSQL 数据库初始化脚本%NC%
echo ===============================================
echo 容器名称: %CONTAINER_NAME%
echo 数据库名称: %DB_NAME%
echo 数据库用户: %DB_USER%
echo 数据库端口: %DB_PORT%
echo 主机映射端口: %HOST_PORT%
echo SQL文件路径: %INIT_SQL%
echo ===============================================

:: 检查是否安装了 Docker
where docker >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo %RED%Error: Docker is not installed. Please install Docker Desktop for Windows first.%NC%
    exit /b 1
)

:: 检查 SQL 文件是否存在
if not exist "%INIT_SQL%" (
    echo %RED%Error: SQL file '%INIT_SQL%' not found!%NC%
    exit /b 1
)

:: 检查容器是否已存在
docker ps -a | findstr "%CONTAINER_NAME%" >nul
if %ERRORLEVEL% equ 0 (
    echo %YELLOW%Warning: Container '%CONTAINER_NAME%' already exists%NC%
    set /p REPLY="Do you want to remove it? (y/N) "
    if /i "!REPLY!"=="y" (
        echo Removing existing container...
        docker stop "%CONTAINER_NAME%" >nul 2>nul
        docker rm "%CONTAINER_NAME%" >nul 2>nul
    ) else (
        echo Operation cancelled
        exit /b 0
    )
)

:: 检查端口是否被占用
netstat -ano | findstr ":%HOST_PORT%" | findstr "LISTENING" >nul
if %ERRORLEVEL% equ 0 (
    echo %YELLOW%Warning: Port %HOST_PORT% is already in use%NC%
    set /p REPLY="Do you want to use a different port? (y/N) "
    if /i "!REPLY!"=="y" (
        set /p HOST_PORT="Enter new port number: "
    ) else (
        echo Operation cancelled
        exit /b 0
    )
)

echo %GREEN%Starting PostgreSQL container...%NC%
docker run --name "%CONTAINER_NAME%" ^
    -e POSTGRES_USER="%DB_USER%" ^
    -e POSTGRES_PASSWORD="%DB_PASSWORD%" ^
    -e POSTGRES_DB="%DB_NAME%" ^
    -p "%HOST_PORT%:%DB_PORT%" ^
    -v "%SQL_DIR%:/docker-entrypoint-initdb.d" ^
    -d ankane/pgvector:latest

if %ERRORLEVEL% neq 0 (
    echo %RED%Error: Failed to start container%NC%
    exit /b 1
)

echo %GREEN%Waiting for PostgreSQL to be ready...%NC%
timeout /t 5 /nobreak >nul

:: 等待 PostgreSQL 准备就绪
set /a retries=10
:WAIT_LOOP
if %retries% leq 0 (
    echo %RED%Error: PostgreSQL startup timeout%NC%
    exit /b 1
)

docker exec %CONTAINER_NAME% pg_isready -U %DB_USER% -d %DB_NAME% >nul 2>nul
if %ERRORLEVEL% neq 0 (
    set /a retries-=1
    echo Waiting for PostgreSQL to start, remaining attempts: %retries%
    timeout /t 3 /nobreak >nul
    goto WAIT_LOOP
)

echo.
echo %GREEN%PostgreSQL container started successfully!%NC%
echo Container name: %CONTAINER_NAME%
echo Connection Information:
echo   Host: localhost
echo   Port: %HOST_PORT%
echo   User: %DB_USER%
echo   Password: %DB_PASSWORD%
echo   Database: %DB_NAME%
echo   Connection URL: jdbc:postgresql://localhost:%HOST_PORT%/%DB_NAME%
echo.
echo You can connect to the database using:
echo   docker exec -it %CONTAINER_NAME% psql -U %DB_USER% -d %DB_NAME%
echo.
echo %GREEN%Database initialization complete!%NC%

endlocal 