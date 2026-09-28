// 浮层提示
//
// 报错不写在表单内部，是因为那会**把容器撑高**——用户点一下提交，
// 按钮和下面的文字全都往下跳一截。浮层不占布局，出现在视野里然后就消失
//
// 放在 store 里是为了让任何地方都能弹，不用一层层往下传回调

import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 自动消失的时间。太长会挡住后面的操作，太短看不清 */
const DURATION_MS = 2600

export const useToastStore = defineStore('toast', () => {
  const messages = ref([])
  let nextId = 1

  function show(text, type = 'error') {
    // 同一条消息连续弹的时候只留一条。连点提交按钮不该叠出一摞一模一样的提示
    const existing = messages.value.find((m) => m.text === text)
    if (existing) {
      dismiss(existing.id)
    }

    const id = nextId++
    messages.value.push({ id, text, type })
    setTimeout(() => dismiss(id), DURATION_MS)
    return id
  }

  function dismiss(id) {
    messages.value = messages.value.filter((m) => m.id !== id)
  }

  return { messages, show, dismiss }
})
