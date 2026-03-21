# 修复 @typescript-eslint/no-explicit-any 警告

## 待修复文件

### 1. util.ts
- [ ] 第571行: `let request: any` - 需要定义具体的请求接口类型

### 2. common_antv.ts
- [ ] 第431行: `Record<string, any> | boolean` - getXAxis 返回类型
- [ ] 第516行: `Record<string, any> | boolean` - getYAxis 返回类型
- [ ] 第618行: `Record<string, any> | boolean` - getYAxisExt 返回类型
- [ ] 第1219行: `this.controlOption as any` - CustomZoom 类
- [ ] 第1228行: `option as any` - CustomZoom 类
- [ ] 第1289行: `newZoomOptions as any` - 地图缩放配置
- [ ] 第1305行: `newZoomOptions as any` - 地图缩放配置
- [ ] 第1321行: `newZoomOptions as any` - 地图缩放配置
- [ ] 第1330行: `newZoomOptions as any` - 地图缩放配置
- [ ] 第1418行: `zoomOptions as any` - 地图缩放配置
- [ ] 第2531行: `scene.map?.setStatus({...} as any)` - 地图状态配置

### 3. common_table.ts
- [ ] 第969行: `as Record<string, any>[]` - 表格数据处理

## 类型定义策略

1. **util.ts**: 定义 `ExcelExportRequest` 接口
2. **common_antv.ts**: 定义 `AxisConfig` 类型和 `ZoomOptions` 接口
3. **common_table.ts**: 使用 `TableRow` 类型替代 `Record<string, any>`

## 进度

- [ ] util.ts 修复完成
- [ ] common_antv.ts 修复完成
- [ ] common_table.ts 修复完成
- [ ] 验证修复结果

## Review

(待完成后填写)
