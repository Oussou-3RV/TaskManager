import { ref } from 'vue'
import { defineStore } from 'pinia'

let nextId = 1

export const useNotificationStore = defineStore('notification', () => {
  const toasts = ref([])

  function add(message, type = 'info', duration = 4000) {
    const id = nextId++
    toasts.value.push({ id, message, type })
    if (duration > 0) {
      setTimeout(() => remove(id), duration)
    }
    return id
  }

  function remove(id) {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  function clear() {
    toasts.value = []
  }

  function success(message, duration) {
    return add(message, 'success', duration)
  }

  function error(message, duration) {
    return add(message, 'danger', duration)
  }

  return { toasts, add, remove, clear, success, error }
})
