import axios from 'axios'

export const AUTH_STORAGE_KEY = 'taskmanager_auth'

const api = axios.create({
  baseURL: 'http://localhost:8080',
})

api.interceptors.request.use((config) => {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (raw) {
    const { token } = JSON.parse(raw)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
  }
  config.headers['X-Trace-Id'] = crypto.randomUUID()
  config.metadata = { clientStart: performance.now() }
  return config
})

// Decompose la latence cote navigateur (reseau + traitement) et logue la
// decomposition backend renvoyee via l'en-tete Server-Timing, correlees par
// trace_id. Voir load-testing/ pour l'analyse agregee sous charge.
function logTiming(response) {
  const clientMs = performance.now() - (response.config.metadata?.clientStart ?? performance.now())
  const traceId = response.headers['x-trace-id']
  const serverTiming = response.headers['server-timing']
  if (traceId) {
    console.debug(
      `[trace ${traceId}] client=${clientMs.toFixed(1)}ms ${serverTiming ? `| server: ${serverTiming}` : ''}`,
    )
  }
}

api.interceptors.response.use(
  (response) => {
    logTiming(response)
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

// Le backend renvoie les erreurs au format application/problem+json (title/detail).
export function errorMessage(err, fallback) {
  return err.response?.data?.detail || fallback
}

export default api
