# 为 resize 事件监听器添加 debounce/throttle

## TODO

- [x] 1. DeCanvas.vue - 添加 `throttle` 导入，包装 `canvasSizeInit`
- [x] 2. DePreview.vue - 添加 `debounce` 导入，包装 `restore`
- [x] 3. useWatermark.ts - 添加 `debounce` 导入，包装 `func`

## Review

### 改动总结

三个文件均添加了 `lodash-es` 的 debounce/throttle 包装，防止 resize 事件频繁触发导致性能问题。

### 文件改动

1. **`src/views/canvas/DeCanvas.vue`**
   - 添加 `import { throttle } from 'lodash-es'`
   - 创建 `throttledCanvasSizeInit = throttle(canvasSizeInit, 300)`
   - `window.addEventListener('resize', ...)` 和 `erd.listenTo` 回调均使用节流版本
   - `onBeforeUnmount` 中添加 `window.removeEventListener('resize', throttledCanvasSizeInit)` 清理
   - eventBus 上的 `canvasSizeInit` 保持原样（非 resize 事件，不需要节流）

2. **`src/components/data-visualization/canvas/DePreview.vue`**
   - 添加 `import { debounce } from 'lodash-es'`
   - 创建 `debouncedRestore = debounce(restore, 200)` 用于 `window.addEventListener('resize', ...)`
   - `erd.listenTo` 中的 `restore` 保持原样（元素 resize 由 element-resize-detector 内部控制频率）

3. **`src/hooks/web/useWatermark.ts`**
   - 添加 `import { debounce } from 'lodash-es'`
   - `setWatermark` 中 `func` 改为 `debounce(() => createWatermark(str), 300)`
   - `clear()` 中 `removeEventListener('resize', func)` 引用同一变量，cleanup 正常工作
