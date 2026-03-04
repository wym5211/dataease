# 修复数据集页面访问问题

## 问题描述
访问数据集页面时出现以下问题：
1. Vue Router无限循环警告
2. 页面重定向回工作台
3. 数据集API返回500错误

## 根本原因
数据库中**缺失基础菜单数据**，包括：
- 工作台 (/workbranch)
- 仪表板 (/panel)
- 数据大屏 (/screen)
- 数据准备 (/data) - 父菜单
- **数据集 (/dataset)** ← 关键缺失
- 数据源 (/datasource)

前端使用动态路由，菜单数据缺失导致路由无法注册。

## 修复步骤

### 方法1：通过数据库客户端执行（推荐）

1. 使用MySQL客户端连接到数据库
```bash
mysql -h127.0.0.1 -uroot -p123456 dataease10
```

2. 执行以下SQL（已创建迁移文件）：
```bash
source E:/cursor/dataease/core/core-backend/src/main/resources/db/migration/V2.11.6__fix_menu_data.sql
```

或者直接复制执行：
```sql
-- 修复缺失的菜单数据
DELETE FROM core_menu WHERE id IN (1, 2, 3, 4, 5, 6);

INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES
(1, 0, 2, 'workbranch', 'workbranch', 1, NULL, '/workbranch', 0, 1, 1),
(2, 0, 2, 'panel', 'visualized/view/panel', 2, NULL, '/panel', 0, 1, 1),
(3, 0, 2, 'screen', 'visualized/view/screen', 3, NULL, '/screen', 0, 1, 1),
(4, 0, 1, 'data', NULL, 4, NULL, '/data', 0, 1, 0),
(5, 4, 2, 'dataset', 'visualized/data/dataset', 1, NULL, '/dataset', 0, 1, 1),
(6, 4, 2, 'datasource', 'visualized/data/datasource', 2, NULL, '/datasource', 0, 1, 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6);
```

### 方法2：重启后端让Flyway自动执行迁移

1. 停止当前后端服务
2. 确保迁移文件存在：`core/core-backend/src/main/resources/db/migration/V2.11.6__fix_menu_data.sql`
3. 重新启动后端服务（Flyway会自动执行新的迁移文件）

## 验证修复

修复后，验证菜单数据是否正常：

```bash
# 检查菜单API
curl -H "X-DE-TOKEN: <your-token>" http://localhost:8100/de2api/menu/query

# 应该返回包含以下菜单的完整列表：
# - ID 1: workbranch
# - ID 2: panel
# - ID 3: screen
# - ID 4: data
# - ID 5: dataset
# - ID 6: datasource
```

## 测试访问

1. 使用admin账号登录系统
2. 点击"数据准备"菜单
3. 应该能正常进入数据集页面，不再出现无限循环或重定向

## 相关文件

- SQL迁移文件：`core/core-backend/src/main/resources/db/migration/V2.11.6__fix_menu_data.sql`
- 权限检查代码：`sdk/common/src/main/java/io/dataease/utils/AuthUtils.java`
- 菜单服务代码：`core/core-backend/src/main/java/io/dataease/menu/server/MenuServer.java`
