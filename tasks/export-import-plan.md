# 导出导入功能实施计划

## 功能需求
增加数据源、数据集、仪表板、数据大屏的导出导入功能，支持整体打包迁移。

## 需求分析

### 现有基础设施
1. **导出中心 (Export Center)** - 导出Excel、数据集等
2. **模板导入导出** - 支持 `.DET2` 和 `.DET2APP` 格式（仪表板/大屏）
3. **数据源Excel处理** - 部分支持

### 缺失功能
- 统一的打包导出格式
- 数据源整体导出/导入
- 数据集整体导出/导入
- 仪表板/大屏的整体打包导出（包含关联的数据集）
- 导入时的依赖关系处理

---

## 技术方案

### 导出文件格式
```json
{
  "version": "2.0",
  "type": "datasource|dataset|dashboard|dataview",
  "exportTime": "2026-03-21T10:00:00",
  "data": {
    // 具体内容
  }
}
```

### 支持的导出类型
1. **datasource** - 数据源
2. **dataset** - 数据集
3. **dashboard** - 仪表板
4. **dataview** - 数据大屏
5. **combined** - 组合导出（包含所有依赖）

---

## TODO 列表

### 第一阶段：后端基础架构

#### 1. [ ] 创建统一的导出导入API接口
- **文件**: `sdk/api/api-base/src/main/java/io/dataease/api/backup/BackupCenterApi.java`
- **内容**:
  - `exportData(BackupRequest)` - 导出请求
  - `importData(BackupRequest)` - 导入请求
  - `generateDownloadUri(BackupRequest)` - 生成下载链接
  - `validateImport(BackupRequest)` - 验证导入数据

#### 2. [ ] 创建DTO模型
- **文件**: `sdk/common/src/main/java/io/dataease/model/backup/`
- **内容**:
  - `BackupRequest.java` - 导出/导入请求
  - `BackupResponse.java` - 导出/导入响应
  - `BackupFileVO.java` - 备份文件信息
  - `ExportPackage.java` - 导出包结构

#### 3. [ ] 创建备份管理Service
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/manage/BackupCenterManage.java`
- **内容**:
  - 导出流程管理
  - 导入流程管理
  - 文件压缩/解压

---

### 第二阶段：数据源导出导入

#### 4. [ ] 数据源导出Service
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/service/impl/DatasourceBackupService.java`
- **内容**:
  - 查询数据源配置
  - 导出密码加密处理
  - 生成JSON格式导出文件

#### 5. [ ] 数据源导入Service
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/service/impl/DatasourceRestoreService.java`
- **内容**:
  - 验证数据源名称是否冲突
  - 创建或更新数据源
  - 处理依赖关系

---

### 第三阶段：数据集导出导入

#### 6. [ ] 数据集导出Service
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/service/impl/DatasetBackupService.java`
- **内容**:
  - 查询数据集信息（包含SQL定义）
  - 导出数据模型信息
  - 导出关联的数据源ID

#### 7. [ ] 数据集导入Service
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/service/impl/DatasetRestoreService.java`
- **内容**:
  - 验证数据源依赖
  - 处理数据集SQL转换
  - 处理名称冲突

---

### 第四阶段：仪表板/大屏导出导入

#### 8. [ ] 仪表板导出Service
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/service/impl/DashboardBackupService.java`
- **内容**:
  - 查询仪表板配置
  - 打包关联的视图配置
  - 打包联动配置

#### 9. [ ] 大屏导出Service
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/service/impl/DataviewBackupService.java`
- **内容**:
  - 查询大屏配置
  - 打包关联的视图配置
  - 打包样式配置

#### 10. [ ] 仪表板/大屏导入Service
- **文件**: `core/core-backend/src/main/java/io/dataease/backup/service/impl/DashboardRestoreService.java`
- **内容**:
  - 验证依赖（数据集、数据源）
  - 处理视图ID映射
  - 创建或更新仪表板/大屏

---

### 第五阶段：前端UI集成

#### 11. [ ] 创建导出对话框组件
- **文件**: `core/core-frontend/src/components/backup/ExportDialog.vue`
- **内容**:
  - 选择导出类型（数据源/数据集/仪表板/大屏）
  - 选择导出项
  - 导出进度显示

#### 12. [ ] 创建导入对话框组件
- **文件**: `core/core-frontend/src/components/backup/ImportDialog.vue`
- **内容**:
  - 文件选择
  - 预览导入内容
  - 冲突处理选项（覆盖/重命名/跳过）

#### 13. [ ] 集成到各页面
- **数据源页面**: 添加导出/导入按钮
- **数据集页面**: 添加导出/导入按钮
- **仪表板页面**: 添加导出/导入按钮
- **大屏页面**: 添加导出/导入按钮

#### 14. [ ] 创建API客户端
- **文件**: `core/core-frontend/src/api/backup.ts`
- **内容**:
  - `exportData()` - 导出
  - `importData()` - 导入
  - `downloadBackup()` - 下载备份文件

---

## 关键文件参考

| 模块 | Server类位置 |
|------|-------------|
| 数据源 | `core/core-backend/src/main/java/io/dataease/datasource/server/DatasourceServer.java` |
| 数据集 | `core/core-backend/src/main/java/io/dataease/dataset/server/DatasetServer.java` |
| 可视化 | `core/core-backend/src/main/java/io/dataease/visualization/server/DataVisualizationServer.java` |
| 模板 | `core/core-backend/src/main/java/io/dataease/template/service/TemplateManageService.java` |

---

## 实施顺序

1. 后端基础架构（API + DTO）
2. 数据源导出/导入
3. 数据集导出/导入
4. 仪表板/大屏导出/导入
5. 前端UI集成

---

## 风险与注意事项

1. **依赖顺序**: 数据集依赖数据源，仪表板/大屏依赖数据集，导入时需按顺序处理
2. **ID映射**: 导出导入后ID会变化，需要处理视图引用关系
3. **加密处理**: 数据源密码需要加密存储，解密导出
4. **名称冲突**: 导入时需处理同名资源（覆盖/重命名/跳过）
5. **文件大小**: 大型仪表板导出可能较大，需考虑分片传输

---

## 实施状态

### 已完成 ✅

1. **后端基础架构**
   - [x] API接口定义 (`BackupCenterApi.java`)
   - [x] DTO模型 (`BackupRequest.java`, `BackupResponse.java`, `ExportPackage.java`)
   - [x] 备份管理Service (`BackupCenterManage.java`)
   - [x] 数据源导出/导入服务 (`BackupDatasourceService.java`)
   - [x] 数据集导出/导入服务 (`BackupDatasetService.java`)
   - [x] 仪表板/大屏导出/导入服务 (`BackupDashboardService.java`)

2. **前端UI集成**
   - [x] 备份设置页面 (`BackupSettings.vue`)
   - [x] API客户端 (`backup.ts`)
   - [x] 集成到系统设置页面

3. **测试验证** (2026-03-22)
   - [x] 导出功能正常工作
   - [x] 下载功能正常工作 (使用blob下载方式，正确传递认证token)
   - [x] 导出历史显示正常
   - [x] 管理员权限验证正常

### 测试结果

通过浏览器自动化测试验证：
- 导出数据源：成功，生成 `export_xxx.zip` 文件
- 下载文件：成功，HTTP 200 响应
- 权限控制：只有管理员用户(uid=1)可见备份设置标签页
