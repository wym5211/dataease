<template>
  <code-mirror
    :quotaMap="props.linkJumpInfoArray.map(ele => ele.sourceFieldName)"
    ref="myCm"
    dom-id="jumpSetField"
    style="height: 100%"
  ></code-mirror>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref, toRefs } from 'vue'
import CodeMirror from '@/views/visualized/data/dataset/form/CodeMirror.vue'

interface LinkJumpInfoItem {
  sourceFieldId: string
  sourceFieldName: string
  [key: string]: unknown
}

interface LinkJumpInfo {
  content: string
  [key: string]: unknown
}

interface MirrorSelectionRange {
  from: number
}

interface MirrorInstance {
  dispatch: (payload: {
    changes: { from: number; to?: number; insert: string }
    selection?: { anchor: number }
  }) => void
  viewState: {
    state: {
      selection: {
        ranges: MirrorSelectionRange[]
      }
      doc: {
        length: number
      }
    }
  }
  state: {
    doc: {
      toString: () => string
    }
  }
  destroy?: () => void
}

interface CodeMirrorComponentRef {
  codeComInit: () => MirrorInstance
}

const myCm = ref<CodeMirrorComponentRef | null>(null)
const mirror = ref<MirrorInstance | null>(null)
const props = defineProps<{
  linkJumpInfoArray: LinkJumpInfoItem[]
  linkJumpInfo: LinkJumpInfo
}>()

const { linkJumpInfo } = toRefs(props)
const state = reactive({
  name2Auto: [] as string[],
  content: ''
})
const timer = ref<ReturnType<typeof setInterval> | null>(null)

const insertFieldToCodeMirror = (value: string) => {
  mirror.value.dispatch({
    changes: { from: mirror.value.viewState.state.selection.ranges[0].from, insert: value },
    selection: { anchor: mirror.value.viewState.state.selection.ranges[0].from }
  })
}

const setNameIdTrans = (
  from: 'sourceFieldId' | 'sourceFieldName',
  to: 'sourceFieldId' | 'sourceFieldName',
  originName: string,
  name2Auto?: string[]
) => {
  if (!originName) {
    return originName
  }
  let name2Id = originName
  const nameIdMap = props.linkJumpInfoArray.reduce<Record<string, string>>((pre, next) => {
    pre[next[from]] = next[to]
    return pre
  }, {})
  const on = originName.match(/\[(.+?)\]/g) || []
  if (on) {
    on.forEach(itm => {
      const ele = itm.slice(1, -1)
      if (name2Auto) {
        name2Auto.push(nameIdMap[ele])
      }
      name2Id = name2Id.replace(`[${ele}]`, `[${nameIdMap[ele]}]`)
    })
  }
  return name2Id
}

const editorInit = (content: string) => {
  state.name2Auto = []
  if (!mirror.value) {
    mirror.value = myCm.value.codeComInit()
  }
  state.content = setNameIdTrans('sourceFieldId', 'sourceFieldName', content, state.name2Auto)
  mirror.value.dispatch({
    changes: {
      from: 0,
      to: mirror.value.viewState.state.doc.length,
      insert: state.content
    }
  })
  if (timer.value) {
    clearInterval(timer.value)
  }
  timer.value = setInterval(() => {
    const content = mirror.value ? mirror.value.state.doc.toString() : ''
    const contentTrans = setNameIdTrans(
      'sourceFieldName',
      'sourceFieldId',
      content,
      state.name2Auto
    )
    linkJumpInfo.value.content = contentTrans
  }, 1500)
}
defineExpose({
  editorInit,
  insertFieldToCodeMirror
})

onBeforeUnmount(() => {
  if (timer.value) {
    clearInterval(timer.value)
  }
  mirror.value && mirror.value.destroy?.()
})
</script>

<style lang="less" scoped></style>
