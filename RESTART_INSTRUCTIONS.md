# 重启后端服务以应用修复

## 修复内容

已修复 `MenuManage.java`，添加了默认菜单fallback机制。当数据库中缺失基础菜单数据时，系统会自动返回以下默认菜单：

1. 工作台 (/workbranch)
2. 仪表板 (/panel)
3. 数据大屏 (/screen)
4. 数据准备 (/data) - 父菜单
5. **数据集 (/dataset)** ← 已修复
6. 数据源 (/datasource)

## 重启步骤

### Windows 环境

1. 停止当前后端进程：
```bash
# 查找Java进程ID
netstat -ano | findstr :8100

# 终止进程（将<PID>替换为实际PID）
taskkill /F /PID <PID>
```

2. 启动新的后端服务：
```bash
cd E:\cursor\dataease\core\core-backend
java -jar target/CoreApplication.jar --spring.profiles.active=standalone
```

### 验证修复

重启后，验证以下内容：

1. **检查菜单API**：
```bash
# 获取token
TOKEN=$(curl -s -X POST http://localhost:8100/de2api/login/localLogin -H 'Content-Type: application/json' -d '{"username":"admin","password":"123456"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 查看菜单
curl -H "X-DE-TOKEN: $TOKEN" http://localhost:8100/de2api/menu/query
```

应该返回包含以下菜单的完整列表：
- workbranch
- panel
- screen
- data (包含子菜单 dataset 和 datasource)

2. **测试访问**：
   - 使用 admin/123456 登录系统
   - 点击"数据准备"菜单
   - 应该能正常进入数据集页面

## 代码变更

**文件**: `core/core-backend/src/main/java/io/dataease/menu/manage/MenuManage.java`

**变更**: 在 `coreMenus()` 方法中添加了fallback逻辑：
- 检查数据库返回的菜单是否包含基础菜单（ID 1-6）
- 如果缺失，返回硬编码的默认菜单列表
- 添加了 `getDefaultMenus()` 和 `createMenu()` 辅助方法

**优势**:
- 无需修改数据库即可恢复系统功能
- 向后兼容，不影响已有数据
- 代码级别修复，立即生效
