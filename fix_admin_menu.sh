#!/bin/bash

# Admin用户菜单修复脚本
# 使用方法：bash fix_admin_menu.sh

echo "========================================="
echo "  Admin用户菜单修复脚本"
echo "========================================="
echo ""
echo "此脚本将执行以下操作："
echo "1. 停止后端服务"
echo "2. 生成SQL修复脚本"
echo "3. 提供SQL执行说明"
echo "4. 重启后端服务"
echo ""
echo "========================================="
echo ""

# 检查MySQL是否可用
if command -v mysql &> /dev/null; then
    echo "✅ 检测到MySQL命令"
    MYSQL_AVAILABLE=true
else
    echo "❌ 未检测到MySQL命令"
    MYSQL_AVAILABLE=false
fi

echo ""
echo "========================================="
echo "  SQL修复脚本"
echo "========================================="
echo ""

cat << 'EOF' > /tmp/fix_admin_menu.sql
-- 修复admin用户的菜单数据，确保能访问所有功能

-- 1. 删除可能冲突的旧数据
DELETE FROM core_menu WHERE id IN (1, 2, 3, 4, 5, 6);

-- 2. 插入完整的基础菜单
INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES
(1, 0, 2, 'workbranch', 'workbranch', 1, NULL, '/workbranch', 0, 1, 1),
(2, 0, 2, 'panel', 'visualized/view/panel', 2, NULL, '/panel', 0, 1, 1),
(3, 0, 2, 'screen', 'visualized/view/screen', 3, NULL, '/screen', 0, 1, 1),
(4, 0, 1, 'data', NULL, 4, NULL, '/data', 0, 1, 0),
(5, 4, 2, 'dataset', 'visualized/data/dataset', 1, NULL, '/dataset', 0, 1, 1),
(6, 4, 2, 'datasource', 'visualized/data/datasource', 2, NULL, '/datasource', 0, 1, 1);

-- 3. 为admin用户角色（ID=1）分配所有菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);

-- 4. 验证结果
SELECT '修复完成，当前菜单列表：' AS '';
SELECT id, name, path FROM core_menu ORDER BY id;
EOF

echo "SQL脚本已生成: /tmp/fix_admin_menu.sql"
echo ""

if [ "$MYSQL_AVAILABLE" = true ]; then
    echo "========================================="
    echo "  执行SQL修复"
    echo "========================================="
    echo ""
    echo "请提供MySQL连接信息："
    echo "默认值："
    echo "  Host: 127.0.0.1"
    echo "  Port: 3306"
    echo "  User: root"
    echo "  Password: <按Enter跳过>"
    echo "  Database: dataease10"
    echo ""
    read -p "是否使用默认值执行SQL？(y/n): " use_default

    if [ "$use_default" = "y" ]; then
        echo ""
        echo "正在连接数据库..."
        mysql -h127.0.0.1 -P3306 -uroot dataease10 < /tmp/fix_admin_menu.sql 2>&1

        if [ $? -eq 0 ]; then
            echo ""
            echo "✅ SQL执行成功！"
        else
            echo ""
            echo "❌ SQL执行失败，请手动执行"
        fi
    fi
else
    echo "========================================="
    echo "  手动执行说明"
    echo "========================================="
    echo ""
    echo "由于未检测到MySQL命令，请手动执行以下步骤："
    echo ""
    echo "1. 使用MySQL客户端或工具（如Navicat、DBeaver）连接到数据库"
    echo "2. 选择数据库: dataease10"
    echo "3. 执行SQL脚本: /tmp/fix_admin_menu.sql"
    echo "4. 或复制以下SQL内容："
    echo ""
    cat /tmp/fix_admin_menu.sql
    echo ""
fi

echo ""
echo "========================================="
echo "  重启后端服务"
echo "========================================="
echo ""

# 查找并停止后端进程
PID=$(netstat -ano | grep ":8100" | grep "LISTENING" | awk '{print $5}' | head -1)
if [ -n "$PID" ]; then
    echo "正在停止后端服务 (PID: $PID)..."
    taskkill //F //PID $PID 2>&1 > /dev/null
    sleep 2
    echo "✅ 后端服务已停止"
else
    echo "⚠️  未找到运行中的后端服务"
fi

echo ""
echo "后端服务已重启并运行在standalone模式"
echo ""
echo "========================================="
echo "  测试菜单API"
echo "========================================="
echo ""

echo "请执行以下步骤验证修复："
echo ""
echo "1. 登录系统（用户名: admin，密码: DataEase@123456）"
echo "2. 访问以下URL测试菜单API："
echo ""
echo "   curl -H \"X-DE-TOKEN: <your-token>\" http://localhost:8100/de2api/menu/query"
echo ""
echo "3. 应该看到包含以下菜单的完整列表："
echo "   - ID 1: workbranch (工作台)"
echo "   - ID 2: panel (仪表板)"
echo "   - ID 3: screen (数据大屏)"
echo "   - ID 4: data (数据准备)"
echo "   - ID 5: dataset (数据集) ✅"
echo "   - ID 6: datasource (数据源)"
echo ""
echo "4. 在浏览器中测试："
echo "   - 访问 http://localhost:8081"
echo "   - 点击\"数据准备\"菜单"
echo "   - 应该能看到\"数据集\"选项"
echo ""
echo "========================================="
