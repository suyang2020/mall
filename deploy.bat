@echo off
chcp 65001 >nul
echo ============================================
echo  Mall Master 项目 Docker 部署脚本
echo ============================================
echo.

:: 检查 Docker 是否运行
echo [1/4] 检查 Docker 环境...
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Docker 未运行，请先启动 Docker Desktop！
    pause
    exit /b 1
)
echo Docker 环境正常。

:: Maven 构建
echo.
echo [2/4] Maven 编译打包项目...
call mvn clean package -DskipTests -f pom.xml
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Maven 构建失败，请检查错误信息！
    pause
    exit /b 1
)
echo Maven 构建完成。

:: 停止并清理旧容器
echo.
echo [3/4] 停止并清理旧容器...
docker compose down
echo 旧容器已清理。

:: 启动所有服务
echo.
echo [4/4] 启动所有 Docker 服务...
docker compose up -d --build
if %ERRORLEVEL% NEQ 0 (
    echo [错误] Docker 服务启动失败！
    pause
    exit /b 1
)

echo.
echo ============================================
echo  部署完成！服务访问地址如下：
echo ============================================
echo.
echo  基础设施：
echo    MySQL:       localhost:3307  (root/root)
echo    Redis:       localhost:6379
echo    RabbitMQ:    http://localhost:15672  (mall/mall, vhost=/mall)
echo    MinIO:       http://localhost:9001  (minioadmin/minioadmin)
echo    Kibana:      http://localhost:5601
echo    Nginx:       http://localhost:80
echo.
echo  应用服务：
echo    mall-admin:  http://localhost:8080/swagger-ui.html
echo    mall-search: http://localhost:8081/swagger-ui.html
echo    mall-portal: http://localhost:8085/swagger-ui.html
echo.
echo  查看日志：
echo    docker compose logs -f mall-admin
echo    docker compose logs -f mall-search
echo    docker compose logs -f mall-portal
echo.
pause
