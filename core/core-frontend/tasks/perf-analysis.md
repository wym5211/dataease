# DataEase 前端性能分析报告

## 1. 重复依赖: lodash 和 lodash-es 同时存在

**严重程度: 中**

`package.json` 同时声明了 `lodash` (^4.17.21) 和 `lodash-es` (^4.17.21)。

- `lodash-es` 被广泛使用：**146 处**导入（推荐方式，支持 tree-shaking）
- `lodash` 被使用：**7 处**导入

**使用 lodash 的文件：**
| 文件 | 行号 | 导入方式 |
|------|------|----------|
| `src/router/establish.ts` | 2 | `import { cloneDeep } from 'lodash'` |
| `src/components/data-visualization/canvas/CanvasCore.vue` | 33 | `import _ from 'lodash'` |
| `src/components/visualization/common/ComponentPosition.vue` | 104 | `import _ from 'lodash'` |
| `src/views/common/DeResourceTree.vue` | 50 | `import _ from 'lodash'` |
| `src/utils/treeSortUtils.ts` | 2 | `import _ from 'lodash'` |
| `src/views/chart/components/editor/dataset-select/DatasetSelect.vue` | 10 | `import _ from 'lodash'` |
| `src/views/chart/components/editor/js/panel/common/common_antv.ts` | 17 | `import _ from 'lodash'` |

**特别注意：** `DeResourceTree.vue` 同时导入了 `lodash-es` (第10行 `throttle`) 和 `lodash` (第50行 `_`)。

**建议：** 将所有 7 处 `lodash` 导入迁移到 `lodash-es`，然后从 `package.json` 移除 `lodash`。由于 `distributed.ts` 的 manualChunks 中仍然引用了 `lodash`，也需要同步更新。

---

## 2. 重复依赖: vuedraggable 和 vue-draggable-next

**严重程度: 低**

- `vuedraggable` (^4.1.0): 被 **15 处**导入使用
- `vue-draggable-next` (^2.2.1): **0 处**导入使用

**建议：** 从 `package.json` 移除 `vue-draggable-next`，它完全没有被使用。

---

## 3. 重复依赖: ace-builds 和 vue3-ace-editor

**严重程度: 低（非重复，有依赖关系）**

- `ace-builds` 被 1 处导入：`src/views/visualized/data/datasource/form/ace-config.ts` (第1行)
- `vue3-ace-editor` 被 1 处导入：`src/views/visualized/data/datasource/form/CodeEdit.vue` (第4行)

**建议：** 这是正常的依赖关系（vue3-ace-editor 依赖 ace-builds），无需修改。

---

## 4. 未使用的依赖

**严重程度: 高**

以下依赖声明在 `package.json` 中但在 `src/` 目录中完全没有被导入：

| 依赖 | 版本 | 说明 |
|------|------|------|
| `@babel/runtime` | ^7.5.5 | **完全未使用**，可能被某些旧版转译器需要 |
| `net` | ^1.0.2 | **完全未使用**，这是一个 Node.js 内置模块的 npm 包，在浏览器端无意义 |
| `screenfull` | ^6.0.2 | **完全未使用**，0 处导入 |
| `html-to-image` | ^1.11.11 | **完全未使用**，0 处导入（项目使用了 `html2canvas` 和 `modern-screenshot`） |
| `vue-draggable-next` | ^2.2.1 | **完全未使用**（见第2项） |

**已使用但可考虑替代的依赖：**
| 依赖 | 导入次数 | 说明 |
|------|----------|------|
| `vue-uuid` | 2 处 | 可用 `crypto.randomUUID()` 原生 API 替代 |
| `snowflake-id` | 1 处 | 仅在 `src/views/visualized/data/dataset/form/util.ts` 使用 |
| `web-storage-cache` | 1 处 | 仅在 `src/hooks/web/useCache.ts` 使用 |

**建议：** 立即移除 `net`、`screenfull`、`html-to-image`、`vue-draggable-next`。检查 `@babel/runtime` 是否为其他包的依赖。考虑用 `crypto.randomUUID()` 替换 `vue-uuid`。

---

## 5. 重型同步导入

**严重程度: 高**

以下重型库在模块顶层同步导入，会阻塞初始加载：

### 5.1 `imgUtils.ts` - 最严重
**文件:** `src/utils/imgUtils.ts` (第1-9行)

```typescript
import html2canvas from 'html2canvas'     // ~300KB
import JsPDF from 'jspdf'                 // ~280KB
import { domToPng } from 'modern-screenshot' // ~100KB
```

这三个库只在导出函数 `downloadCanvas`、`downloadCanvas2`、`download2AppTemplate` 中使用，这些是用户点击下载/导出时才触发的操作。

**建议：** 将这三个导入改为动态 `import()`：
```typescript
export async function downloadCanvas(type, canvasDom, name, callBack?) {
  const { default: html2canvas } = await import('html2canvas')
  const { default: JsPDF } = await import('jspdf')
  // ...
}
```

### 5.2 `common_table.ts` - ExcelJS
**文件:** `src/views/chart/components/js/panel/common/common_table.ts` (第63行)

```typescript
import Exceljs from 'exceljs'  // ~800KB
```

仅在表格导出 Excel 功能中使用。

**建议：** 改为动态导入，仅在用户点击导出时加载。

### 5.3 `Component.vue` (de-video) - Video Player
**文件:** `src/custom-component/de-video/Component.vue` (第13行)

```typescript
import { VideoPlayer } from '@videojs-player/vue'  // 引入整个 video.js
```

**建议：** 使用 `defineAsyncComponent` 异步加载视频组件。

### 5.4 `Component.vue` (de-stream-media) - flv.js
**文件:** `src/custom-component/de-stream-media/Component.vue` (第22行)

```typescript
import flvjs from 'flv.js'  // ~200KB
```

**建议：** 改为动态导入，仅在需要播放流媒体时加载。

### 5.5 `common_antv.ts` 和多个图表文件 - mathjs
**文件（部分）:**
- `src/utils/translate.ts` (第1行): `import { divide, multiply, floor } from 'mathjs'`
- `src/utils/changeComponentsSizeWithScale.ts` (第2行): `import { divide, multiply } from 'mathjs'`
- `src/views/chart/components/js/panel/common/common_antv.ts` (第15行): `import { add } from 'mathjs'`

mathjs 非常庞大（~1MB+），但只使用了 `add`、`divide`、`multiply`、`floor` 四个函数。

**建议：** 用原生 JavaScript 运算替代：
```typescript
// 替代 mathjs.add(a, b)
const add = (a, b) => a + b
// 替代 mathjs.divide(a, b) - 通常用于精度计算
const divide = (a, b) => a / b
```
如果需要精度计算，可使用更轻量的 `decimal.js`（项目已有此依赖）。

### 5.6 `@antv/s2` 和 `@antv/l7` - 同步导入
虽然 `@antv/g2plot` 的图表已经在使用动态 `import()`（见 `src/views/chart/components/js/panel/charts/` 下的 23 处动态导入），但以下库仍然是同步导入：

- `@antv/s2`: 12 处同步导入（表格组件相关）
- `@antv/l7`: 4 处同步导入（地图组件相关，仅导入子模块如 `Zoom`、`Popup`）

**建议：** `@antv/s2` 作为表格渲染核心库，较难改为动态导入（除非整个表格组件都用 `defineAsyncComponent` 包裹）。`@antv/l7` 的子模块导入相对较小，可保持现状。

---

## 6. 缺少防抖/节流的事件监听器

**严重程度: 中**

### 6.1 `DeCanvas.vue` - window resize 未防抖
**文件:** `src/views/canvas/DeCanvas.vue` (第244行)

```typescript
window.addEventListener('resize', canvasSizeInit)
```

`canvasSizeInit` 调用了 `dashboardCanvasSizeInit()` -> `cyGridster.value.canvasSizeInit()` -> `scaleInit()`，涉及 DOM 操作和布局重算，但没有 debounce。

同样在 `elementResizeDetectorMaker` 回调中也没有防抖：
```typescript
erd.listenTo(document.getElementById(domId.value), () => {
  canvasSizeInit()  // 没有防抖
})
```

**建议：** 用 `lodash-es/throttle` 包裹 `canvasSizeInit`（resize 事件推荐用 throttle 而非 debounce）。

### 6.2 `DePreview.vue` - window resize 未防抖
**文件:** `src/components/data-visualization/canvas/DePreview.vue` (第432行)

```typescript
window.addEventListener('resize', restore)
```

`restore` -> `resetLayout()` 做了布局重算和缩放比例计算，但没有 debounce。

同样 `erd.listenTo` 回调中也直接调用了 `restore()` 和 `initWatermark()`。

**建议：** 用 debounce 包裹 `restore`。

### 6.3 `useWatermark.ts` - resize 未防抖
**文件:** `src/hooks/web/useWatermark.ts` (第51行)

```typescript
window.addEventListener('resize', func)  // func = createWatermark
```

`createWatermark` 创建了一个 Canvas 并生成 dataURL，在 resize 时会频繁触发。

**建议：** 用 debounce 包裹 `func`。

---

## 7. Vite 构建优化

### 7.1 manualChunks 策略差异巨大

**base 模式** (`config/base.ts` 第22-25行):
```typescript
manualChunks(id: string) {
  if (id.includes('node_modules')) {
    return id.toString().split('node_modules/')[1].split('/')[0].toString()
  }
}
```
这会为每个 `node_modules` 下的顶级包创建一个单独的 chunk，产生**数百个小文件**。

**distributed 模式** (`config/distributed.ts` 第24-33行):
```typescript
manualChunks: {
  echarts: ['echarts'],
  vue: ['vue', 'vue-router', 'pinia', 'vue-i18n', 'mitt'],
  lodash: ['lodash-es', 'lodash'],
  library: ['jspdf', '@tinymce/tinymce-vue', 'screenfull'],
  antv: ['@antv/g2', '@antv/g2plot', '@antv/l7', '@antv/l7plot', '@antv/s2'],
  tinymce: ['tinymce'],
  axios: ['axios'],
  'vuedraggable-es': ['vuedraggable']
}
```
这会创建**8 个有意义的 chunk**，减少了 HTTP 请求数。

**建议：** base 模式应采用与 distributed 模式类似的分块策略。按顶级包名拆分会产生过多小 chunk（如 `@antv`、`@vueuse`、`element-plus-secondary` 等都变成独立 chunk），增加浏览器并发请求数。推荐将常用框架、UI 库、图表库分别打包。

### 7.2 optimizeDeps 不完整

**文件:** `config/common.ts` (第87-95行)

当前仅预构建 6 个包：
```typescript
optimizeDeps: {
  include: [
    'vue',
    'vue-router',
    'vue-types',
    'element-plus-secondary/es/locale/lang/zh-cn',
    'element-plus-secondary/es/locale/lang/en',
    '@vueuse/core',
    'axios'
  ]
}
```

**建议添加到 optimizeDeps.include 的包：**
| 包名 | 原因 |
|------|------|
| `lodash-es` | 被 146 处导入，Vite 预构建可显著提升 dev 启动速度 |
| `dayjs` | 日期处理库，广泛使用 |
| `pinia` | 状态管理核心库 |
| `mitt` | 事件总线，频繁使用 |
| `element-plus-secondary` | UI 库主体 |
| `echarts` | 图表库，虽然可能已有预构建，但显式声明更可靠 |
| `decimal.js` | 精度计算库 |
| `@antv/s2` | 表格渲染库 |
| `@antv/g2plot` | 图表库 |

---

## 8. 大型 Store 文件

**严重程度: 低**

**文件:** `src/store/modules/data-visualization/dvMain.ts` (1805 行)

这是项目中最大的 store 文件，包含大量 state（50+ 属性）和 action（40+ 方法）。虽然 Pinia 的 state 是惰性响应式的（只有被访问的属性才是响应式的），但 store 的初始化和模块加载仍会消耗一定时间。

**建议：** 考虑将一些独立的 action（如模板操作、外部参数过滤等）拆分为独立的 composable 或子 store。但这是一个低优先级的重构项。

---

## 9. Deep Watch 的广泛使用

**严重程度: 低**

在 `src/` 中发现 **30+ 处**使用 `deep: true` 的 `watch`。虽然大部分监听的是局部组件状态（如配置对象），但需要注意：

- `src/views/chart/components/editor/index.vue` (第282行): 监听 `canvasViewInfo` with `deep: true, immediate: true`，这个对象可能很大
- `src/views/data-visualization/PreviewCanvas.vue` (第192行): 监听大型状态

**建议：** 对于大型对象，考虑使用 `watch` 监听特定属性路径而非整个对象，或者使用 `computed` + `watch` 的组合来减少不必要的触发。

---

## 优化优先级总结

| 优先级 | 问题 | 预估影响 |
|--------|------|----------|
| P0 | 移除未使用依赖 (`net`, `screenfull`, `html-to-image`, `vue-draggable-next`) | 减少安装体积 |
| P0 | 将 `html2canvas`, `jspdf`, `modern-screenshot`, `exceljs` 改为动态导入 | 减少首屏加载体积 ~1.5MB |
| P1 | 统一使用 `lodash-es`，移除 `lodash` | 消除重复依赖 |
| P1 | 改善 base 模式的 `manualChunks` 策略 | 减少 HTTP 请求数 |
| P1 | 补充 `optimizeDeps.include` | 提升开发服务器启动速度 |
| P1 | 替换 `mathjs` 为原生运算或 `decimal.js` | 减少包体积 ~1MB |
| P2 | 为 `DeCanvas.vue`, `DePreview.vue` 的 resize 添加防抖 | 减少 resize 时的计算开销 |
| P2 | `flv.js`, `video.js` 组件改为异步加载 | 减少首屏加载 |
| P3 | 用 `crypto.randomUUID()` 替换 `vue-uuid` | 代码简化 |
| P3 | 优化 deep watch | 减少不必要的响应式触发 |
