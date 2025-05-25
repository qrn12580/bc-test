#!/bin/bash

# 区块链安全管理系统启动脚本
# Blockchain Security Management System Startup Script

echo "=========================================="
echo "区块链安全管理系统启动脚本"
echo "Blockchain Security Management System"
echo "=========================================="

# 检查Java环境
echo "检查Java环境..."
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请安装Java 8或更高版本"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Java版本: $JAVA_VERSION"

# 检查Maven环境
echo "检查Maven环境..."
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven，请安装Maven 3.6或更高版本"
    exit 1
fi

MVN_VERSION=$(mvn -version | head -n 1)
echo "Maven版本: $MVN_VERSION"

# 检查MySQL连接
echo "检查MySQL连接..."
if ! command -v mysql &> /dev/null; then
    echo "警告: 未找到MySQL客户端，请确保MySQL服务器正在运行"
else
    echo "MySQL客户端已安装"
fi

# 创建日志目录
echo "创建日志目录..."
mkdir -p logs

# 编译项目
echo "编译项目..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

echo "项目编译成功"

# 检查数据库配置
echo "检查数据库配置..."
if [ ! -f "src/main/resources/application.properties" ]; then
    echo "创建默认数据库配置..."
    cat > src/main/resources/application.properties << EOF
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/blockchain_voting?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=blockchain
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# 服务器配置
server.port=8080
server.servlet.context-path=/

# 日志配置
logging.level.com.blockchain=INFO
logging.file.name=logs/blockchain-security.log

# 安全配置
security.jwt.secret=blockchain_security_jwt_secret_key_2024
security.jwt.expiration=86400000

# 调度任务配置
spring.task.scheduling.pool.size=5
EOF
    echo "已创建默认配置文件，请根据需要修改数据库连接信息"
fi

# 启动应用
echo "启动区块链安全管理系统..."
echo "请稍候，系统正在启动中..."

# 后台启动应用并记录PID
nohup mvn spring-boot:run > logs/startup.log 2>&1 &
APP_PID=$!

echo "应用已启动，PID: $APP_PID"
echo $APP_PID > logs/app.pid

# 等待应用启动
echo "等待应用启动完成..."
sleep 10

# 检查应用是否成功启动
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✓ 应用启动成功！"
        break
    elif [ $i -eq 30 ]; then
        echo "✗ 应用启动超时，请检查日志文件"
        echo "日志文件位置: logs/startup.log"
        exit 1
    else
        echo "等待中... ($i/30)"
        sleep 2
    fi
done

echo ""
echo "=========================================="
echo "系统启动完成！"
echo "=========================================="
echo ""
echo "访问地址:"
echo "  主页: http://localhost:8080"
echo "  安全管理中心: http://localhost:8080/security-dashboard.html"
echo "  区块链操作: http://localhost:8080/BlockChain.html"
echo ""
echo "系统信息:"
echo "  PID: $APP_PID"
echo "  日志文件: logs/blockchain-security.log"
echo "  启动日志: logs/startup.log"
echo ""
echo "停止系统:"
echo "  ./stop.sh"
echo "  或者: kill $APP_PID"
echo ""
echo "数据库初始化:"
echo "  请确保MySQL服务正在运行"
echo "  数据库: blockchain_voting"
echo "  用户: blockchain"
echo "  密码: password"
echo ""
echo "快速开始:"
echo "  1. 访问安全管理中心"
echo "  2. 点击'初始化安全环境'"
echo "  3. 开始使用各项安全功能"
echo ""
echo "=========================================="

# 显示实时日志（可选）
read -p "是否显示实时日志? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "显示实时日志 (按Ctrl+C退出):"
    tail -f logs/blockchain-security.log
fi 