#!/bin/bash

echo "========================================"
echo "    跨域认证系统快速测试脚本"
echo "========================================"
echo

echo "[1/4] 检查Java环境..."
if command -v java &> /dev/null; then
    echo "✅ Java环境正常"
    java -version
else
    echo "❌ Java环境未配置，请安装Java 8+"
    exit 1
fi

echo
echo "[2/4] 检查应用是否运行..."
if curl -s -f http://localhost:8080 > /dev/null 2>&1; then
    echo "✅ 应用已运行"
else
    echo "❌ 应用未运行，请先启动应用"
    echo
    echo "🚀 启动应用选项："
    echo "1. 使用Maven启动：./mvnw spring-boot:run"
    echo "2. 使用JAR启动：java -jar target/blockchain-*.jar"
    echo "3. 在IDE中运行主类"
    exit 1
fi

echo
echo "[3/4] 打开测试工具..."
echo "📖 API测试工具: http://localhost:8080/test-scripts/cross-domain-api-test.html"
echo "🎨 前端界面: http://localhost:8080/crossdomain.html"
echo "📋 登录页面: http://localhost:8080/login.html"

echo
echo "[4/4] 启动浏览器..."

# 尝试打开浏览器
if command -v xdg-open &> /dev/null; then
    # Linux
    xdg-open http://localhost:8080/test-scripts/cross-domain-api-test.html
elif command -v open &> /dev/null; then
    # macOS
    open http://localhost:8080/test-scripts/cross-domain-api-test.html
else
    echo "请手动在浏览器中打开: http://localhost:8080/test-scripts/cross-domain-api-test.html"
fi

echo
echo "========================================"
echo "测试工具已启动！请按照以下步骤测试："
echo
echo "🔸 第一步：注册测试域"
echo "🔸 第二步：建立信任关系"
echo "🔸 第三步：测试跨域认证"
echo "🔸 第四步：验证前端界面"
echo
echo "详细测试指南请参考：test-scripts/测试执行指南.md"
echo "========================================"

read -p "按回车键继续..." 