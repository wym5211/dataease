# 修复 @typescript-eslint/no-explicit-any 警告

## 任务列表

- [ ] 1. 修复 g2plot_tooltip_carousel.ts
- [ ] 2. 修复 util.ts
- [ ] 3. 修复 panel/types/index.ts
- [ ] 4. 修复 panel/types/impl/g2plot.ts
- [ ] 5. 修复 panel/types/impl/s2.ts
- [ ] 6. 修复 panel/types/impl/l7.ts
- [ ] 7. 修复 panel/types/impl/l7plot.ts
- [ ] 8. 修复 panel/common/common_antv.ts
- [ ] 9. 修复 panel/common/common_table.ts

## 分析

### 1. g2plot_tooltip_carousel.ts
- 第77行: `timers = { interval: null, carousel: null }` - 使用 `null` 类型
- 第336行: `getPieTooltipPosition(view, value: string)` - view 参数缺少类型
- 第371行: `getDualAxesTooltipPosition(view, value: string)` - view 参数缺少类型
- 第397行: `(data: any) => data[this.config.xField] === value` - data 需要类型
- 第409行: `(data: any) => data[this.config.xField] !== value` - data 需要类型
- 第472行: `const addMouseEvent = el =>` - el 参数缺少类型
- 第674-676行: debounce 函数的参数使用 `any[]`

### 2. util.ts
- 第231行: `const remark = {} as any` - 需要定义具体类型
- 第348行: `handleBreakLineMultiDimension(data)` - data 参数缺少类型
- 第416行: `const _temp = {...} as any` - 需要定义具体类型
- 第483行: `isParent(type: any, parentType: any)` - 参数使用 any

### 3. panel/types/index.ts
- 第23-24行: `render: () => any`, `destroy: () => any` - 返回类型
- 第44行: `protected defaultData: any[]` - 数组元素类型
- 第50-51行: 构造函数参数 `defaultData?: any[]`
- 第76行: `action?: (...args: any[]) => any` - 回调函数类型
- 第116行: 构造函数参数 `defaultData?: any[]`
- 第125行: 构造函数参数 `defaultData: any[]`

### 4. panel/types/impl/g2plot.ts
- 第45行: `quadrantDefaultBaseline?: (...args: any) => void` - 回调函数类型
- 第170, 174行: `data?: any[]` - 数据类型
- 第190行: `data: any[]` - 数据类型
- 第194行: `configEmptyDataStyle(newData, container, newChart?, content?)` - 参数缺少类型
- 第203行: `context?: Record<string, any>` - 使用 Record
- 第204行: `defaultData: any[]` - 数据类型

### 5. panel/types/impl/s2.ts
- 第37-38行: `resizeAction?: (...args: any) => void`, `touchAction?: (...args: any) => void`
- 第42行: `defaultData: any[]`
- 第73行: `event` 参数缺少类型

### 6. panel/types/impl/l7.ts
- 第23行: `[key: string]: string | any` - 索引签名
- 第85行: 返回类型 `| any`
- 第110行: `mapKey?: any`
- 第126行: `defaultData: any[]`

### 7. panel/types/impl/l7plot.ts
- 第90行: `defaultData?: any[]`
- 第94行: `context?: Record<string, any>`

### 8. panel/common/common_antv.ts (需要进一步分析)
### 9. panel/common/common_table.ts (需要进一步分析)

## Review

待完成后填写。
