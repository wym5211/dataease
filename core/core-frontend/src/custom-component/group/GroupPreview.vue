<script setup lang="ts">
import { ref, toRefs, type PropType } from 'vue'
import ComponentWrapper from '@/components/data-visualization/canvas/ComponentWrapper.vue'
import { toPercent } from '@/utils/translate'
import { dvMainStoreWithOut } from '@/store/modules/data-visualization/dvMain'
import UserViewEnlarge from '@/components/visualization/UserViewEnlarge.vue'
const dvMainStore = dvMainStoreWithOut()
const userViewEnlargeRef = ref<InstanceType<typeof UserViewEnlarge> | null>(null)
const AnyComponentWrapper = ComponentWrapper as typeof ComponentWrapper

const props = defineProps({
  propValue: {
    type: Array as PropType<unknown[]>,
    default: () => []
  },
  element: {
    type: Object as PropType<Record<string, unknown>>,
    default() {
      return {
        propValue: null
      }
    }
  },
  showPosition: {
    type: String,
    required: false,
    default: 'canvas'
  },
  dvInfo: {
    type: Object as PropType<Record<string, unknown>>,
    required: true
  },
  // 仪表板刷新计时器
  searchCount: {
    type: Number,
    required: false,
    default: 0
  },
  scale: {
    type: Number,
    required: false,
    default: 1
  },
  canvasViewInfo: {
    type: Object as PropType<Record<string, unknown>>,
    required: true
  },
  // 字体
  fontFamily: {
    type: String,
    required: false,
    default: 'inherit'
  }
})

const { propValue, dvInfo, searchCount, scale, canvasViewInfo } = toRefs(props)
const customGroupStyle = (item: Record<string, unknown>) => {
  return {
    width: toPercent(item.groupStyle.width),
    height: toPercent(item.groupStyle.height),
    top: toPercent(item.groupStyle.top),
    left: toPercent(item.groupStyle.left)
  }
}

const userViewEnlargeOpen = (opt: unknown, item: Record<string, unknown>) => {
  userViewEnlargeRef.value.dialogInit(
    dvMainStore.canvasStyleData,
    canvasViewInfo.value[item.id],
    item,
    opt,
    { scale: scale.value }
  )
}
</script>

<template>
  <div class="group">
    <div>
      <AnyComponentWrapper
        v-for="(item, index) in propValue"
        :id="'component' + item.id"
        :view-info="canvasViewInfo[item.id]"
        :key="index"
        :config="item"
        :index="index"
        :dv-info="dvInfo"
        :canvas-view-info="canvasViewInfo"
        :style="customGroupStyle(item)"
        :show-position="showPosition"
        :search-count="searchCount"
        :scale="scale"
        :font-family="fontFamily"
        @userViewEnlargeOpen="userViewEnlargeOpen($event, item)"
      />
    </div>
    <user-view-enlarge ref="userViewEnlargeRef"></user-view-enlarge>
  </div>
</template>

<style lang="less" scoped>
.group {
  & > div {
    position: relative;
    width: 100%;
    height: 100%;

    .component {
      position: absolute;
    }
  }
}
</style>
