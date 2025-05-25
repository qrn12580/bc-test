#!/bin/bash

# 区块链安全管理系统停止脚本
# Blockchain Security Management System Stop Script

echo "=========================================="
echo "区块链安全管理系统停止脚本"
echo "Blockchain Security Management System Stop"
echo "=========================================="

# 检查PID文件是否存在
if [ ! -f "logs/app.pid" ]; then
    echo "未找到PID文件，尝试查找运行中的进程..."
    
    # 查找Spring Boot进程
    SPRING_PID=$(ps aux | grep "spring-boot:run" | grep -v grep | awk '{print $2}')
    
    if [ -z "$SPRING_PID" ]; then
        echo "未找到运行中的应用进程"
        exit 1
    else
        echo "找到运行中的进程: $SPRING_PID"
        APP_PID=$SPRING_PID
    fi
else
    # 从PID文件读取进程ID
    APP_PID=$(cat logs/app.pid)
    echo "从PID文件读取进程ID: $APP_PID"
fi

# 检查进程是否存在
if ! ps -p $APP_PID > /dev/null 2>&1; then
    echo "进程 $APP_PID 不存在或已停止"
    # 清理PID文件
    rm -f logs/app.pid
    exit 0
fi

echo "正在停止应用进程 $APP_PID..."

# 优雅停止 - 发送TERM信号
kill -TERM $APP_PID

# 等待进程停止
echo "等待进程优雅停止..."
for i in {1..30}; do
    if ! ps -p $APP_PID > /dev/null 2>&1; then
        echo "✓ 应用已成功停止"
        break
    elif [ $i -eq 30 ]; then
        echo "优雅停止超时，强制终止进程..."
        kill -KILL $APP_PID
        sleep 2
        if ! ps -p $APP_PID > /dev/null 2>&1; then
            echo "✓ 应用已强制停止"
        else
            echo "✗ 无法停止应用进程"
            exit 1
        fi
    else
        echo "等待中... ($i/30)"
        sleep 1
    fi
done

# 清理PID文件
rm -f logs/app.pid

# 查找并停止相关的Maven进程
echo "检查相关的Maven进程..."
MVN_PIDS=$(ps aux | grep "mvn.*spring-boot:run" | grep -v grep | awk '{print $2}')

if [ ! -z "$MVN_PIDS" ]; then
    echo "停止Maven进程: $MVN_PIDS"
    for pid in $MVN_PIDS; do
        kill -TERM $pid 2>/dev/null
    done
    sleep 2
    
    # 检查是否还有残留进程
    MVN_PIDS=$(ps aux | grep "mvn.*spring-boot:run" | grep -v grep | awk '{print $2}')
    if [ ! -z "$MVN_PIDS" ]; then
        echo "强制停止残留Maven进程: $MVN_PIDS"
        for pid in $MVN_PIDS; do
            kill -KILL $pid 2>/dev/null
        done
    fi
fi

# 检查端口占用
echo "检查端口8080占用情况..."
PORT_PID=$(lsof -ti:8080 2>/dev/null)

if [ ! -z "$PORT_PID" ]; then
    echo "端口8080仍被进程 $PORT_PID 占用，尝试停止..."
    kill -TERM $PORT_PID 2>/dev/null
    sleep 2
    
    PORT_PID=$(lsof -ti:8080 2>/dev/null)
    if [ ! -z "$PORT_PID" ]; then
        echo "强制停止占用端口的进程 $PORT_PID"
        kill -KILL $PORT_PID 2>/dev/null
    fi
fi

echo ""
echo "=========================================="
echo "系统停止完成！"
echo "=========================================="
echo ""
echo "系统状态:"
echo "  应用进程: 已停止"
echo "  端口8080: 已释放"
echo ""
echo "日志文件保留在 logs/ 目录中:"
echo "  - logs/blockchain-security.log (应用日志)"
echo "  - logs/startup.log (启动日志)"
echo ""
echo "重新启动系统:"
echo "  ./start.sh"
echo ""
echo "清理所有日志:"
echo "  rm -rf logs/"
echo ""
echo "==========================================" 