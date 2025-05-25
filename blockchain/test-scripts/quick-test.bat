@echo off
chcp 65001 >nul
echo ========================================
echo     跨域认证系统快速测试脚本
echo ========================================
echo.

echo [1/4] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java环境未配置，请安装Java 8+
    pause
    exit /b 1
) else (
    echo ✅ Java环境正常
)

echo.
echo [2/4] 检查应用是否运行...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080' -Method GET -TimeoutSec 5; Write-Host '✅ 应用已运行' } catch { Write-Host '❌ 应用未运行，请先启动应用'; exit 1 }"
if %errorlevel% neq 0 (
    echo.
    echo 🚀 启动应用选项：
    echo 1. 使用Maven启动：./mvnw spring-boot:run
    echo 2. 使用JAR启动：java -jar target/blockchain-*.jar
    echo 3. 在IDE中运行主类
    pause
    exit /b 1
)

echo.
echo [3/4] 打开测试工具...
echo 📖 API测试工具: http://localhost:8080/test-scripts/cross-domain-api-test.html
echo 🎨 前端界面: http://localhost:8080/crossdomain.html
echo 📋 登录页面: http://localhost:8080/login.html

echo.
echo [4/4] 启动浏览器...
start http://localhost:8080/test-scripts/cross-domain-api-test.html

echo.
echo ========================================
echo 测试工具已启动！请按照以下步骤测试：
echo.
echo 🔸 第一步：注册测试域
echo 🔸 第二步：建立信任关系  
echo 🔸 第三步：测试跨域认证
echo 🔸 第四步：验证前端界面
echo.
echo 详细测试指南请参考：test-scripts/测试执行指南.md
echo ========================================

pause 