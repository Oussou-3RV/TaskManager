import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import api, { AUTH_STORAGE_KEY } from '@/services/api'

function loadStoredAuth() {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) return { token: null, username: null, role: null }
  try {
    return JSON.parse(raw)
  } catch {
    return { token: null, username: null, role: null }
  }
}

export const useAuthStore = defineStore('auth', () => {
  const stored = loadStoredAuth()
  const token = ref(stored.token)
  const username = ref(stored.username)
  const role = ref(stored.role)

  const isAuthenticated = computed(() => !!token.value)

  function hasRole(...roles) {
    return roles.includes(role.value)
  }

  function persist() {
    localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({ token: token.value, username: username.value, role: role.value }),
    )
  }

  async function login(credentials) {
    const { data } = await api.post('/api/v1/auth/login', credentials)
    token.value = data.token
    username.value = data.username
    role.value = data.role
    persist()
  }

  function logout() {
    token.value = null
    username.value = null
    role.value = null
    localStorage.removeItem(AUTH_STORAGE_KEY)
  }

  return { token, username, role, isAuthenticated, hasRole, login, logout }
})
