# 动态导入重构计划

## 目标
将重型库的静态导入转换为动态导入，以减少初始包大小。

## 待办事项

- [x] 1. imgUtils.ts - 将 `html2canvas`、`jspdf`、`modern-screenshot` 的静态导入改为函数内动态导入
- [x] 2. common_table.ts - 将 `Exceljs` 的静态导入改为函数内动态导入
- [x] 3. de-stream-media/Component.vue - 将 `flv.js` 的静态导入改为函数内动态导入
- [x] 4. de-video/Component.vue - 将 `VideoPlayer` 改为 `defineAsyncComponent`

## 审查

### 修改摘要

#### 1. `src/utils/imgUtils.ts`
- 移除了顶层的 `import html2canvas from 'html2canvas'`、`import JsPDF from 'jspdf'`、`import { domToPng } from 'modern-screenshot'`
- `download2AppTemplate()` 改为 `async`，内部动态导入 `html2canvas`
- `downloadCanvas()` 改为 `async`，内部动态导入 `html2canvas` 和 `jspdf`
- `downloadCanvas2()` 改为 `async`，内部动态导入 `modern-screenshot` 和 `jspdf`

#### 2. `src/views/chart/components/js/panel/common/common_table.ts`
- 移除了顶层的 `import Exceljs from 'exceljs'`
- 在 `exportGridPivot()`、`exportRowQuotaGridPivot()`、`exportTreePivot()`、`exportRowQuotaTreePivot()` 四个 async 函数内部各自动态导入 `exceljs`

#### 3. `src/custom-component/de-stream-media/Component.vue`
- 移除了顶层的 `import flvjs from 'flv.js'`
- `initOption()` 改为 `async`，内部 `nextTick` 回调也改为 `async`，动态导入 `flv.js`

#### 4. `src/custom-component/de-video/Component.vue`
- 移除了 `import { VideoPlayer } from '@videojs-player/vue'`
- 使用 `defineAsyncComponent(() => import('@videojs-player/vue').then(m => ({ default: m.VideoPlayer })))` 替代
- 模板中的 `<VideoPlayer>` 用法无需改变

### 注意事项
- 所有调用 `download2AppTemplate`、`downloadCanvas`、`downloadCanvas2` 的地方需要注意返回值现在是 Promise
- `defineAsyncComponent` 会在组件首次渲染时加载，Vue 会自动处理 loading 状态
