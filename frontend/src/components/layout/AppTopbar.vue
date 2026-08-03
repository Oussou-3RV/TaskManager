<template>
    <header class="topbar">
        <h1 class="topbar__title">{{ title }}</h1>
        <div class="topbar__actions" ref="menuRoot">
            <template v-if="activeRule">
                <BaseBadge variant="accent">{{ activeRule.policy }}</BaseBadge>
                <span class="topbar__rule-label">Règle active</span>
            </template>

            <button class="topbar__menu-btn" @click="menuOpen = !menuOpen">⋯</button>

            <div v-if="menuOpen" class="topbar__menu">
                <template v-if="auth.hasRole('ADMIN') && rules.length">
                    <span class="topbar__menu-heading">Activer une règle</span>
                    <button
                        v-for="rule in rules"
                        :key="rule.id"
                        class="topbar__menu-item"
                        :disabled="rule.isActive"
                        @click="activateRule(rule)"
                    >
                        {{ rule.name }} <span class="topbar__menu-item-policy">({{ rule.policy }})</span>
                    </button>
                    <div class="topbar__menu-divider" />
                </template>
                <button class="topbar__menu-item" @click="handleLogout">Déconnexion</button>
            </div>
        </div>
    </header>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import api from '@/services/api'
import BaseBadge from '@/components/ui/BaseBadge.vue'

defineProps({
    title: { type: String, default: '' },
})

const auth = useAuthStore()
const notification = useNotificationStore()
const router = useRouter()

const rules = ref([])
const activeRule = ref(null)
const menuOpen = ref(false)
const menuRoot = ref(null)

async function loadRules() {
    try {
        const { data } = await api.get('/api/v1/rules')
        rules.value = data
        activeRule.value = data.find((rule) => rule.isActive) || null
    } catch {
        // badge simplement absent si les règles ne peuvent pas être chargées
    }
}

async function activateRule(rule) {
    try {
        await api.put(`/api/v1/rules/${rule.id}/toggle`)
        await loadRules()
        menuOpen.value = false
        notification.success(`Règle "${rule.name}" activée.`)
    } catch {
        notification.error("Impossible d'activer cette règle.")
    }
}

function handleLogout() {
    auth.logout()
    router.push('/login')
}

function handleClickOutside(event) {
    if (menuRoot.value && !menuRoot.value.contains(event.target)) {
        menuOpen.value = false
    }
}

onMounted(() => {
    loadRules()
    document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
    document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.topbar {
    height: var(--topbar-height);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 var(--space-xl);
    border-bottom: 0.5px solid var(--border);
    background: var(--bg-app);
}

.topbar__title {
    margin: 0;
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--text-primary);
}

.topbar__actions {
    position: relative;
    display: flex;
    align-items: center;
    gap: var(--space-md);
}

.topbar__rule-label {
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

.topbar__menu-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: var(--radius-md);
    border: 0.5px solid var(--border);
    background: var(--bg-surface);
    color: var(--text-secondary);
    font-size: var(--text-lg);
    cursor: pointer;
}

.topbar__menu-btn:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
}

.topbar__menu {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    min-width: 220px;
    background: var(--bg-surface);
    border: 0.5px solid var(--border);
    border-radius: var(--radius-lg);
    padding: var(--space-sm);
    display: flex;
    flex-direction: column;
    gap: 2px;
    z-index: 20;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.topbar__menu-heading {
    font-size: var(--text-xs);
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.03em;
    padding: var(--space-xs) var(--space-sm);
}

.topbar__menu-item {
    text-align: left;
    padding: var(--space-sm);
    border-radius: var(--radius-sm);
    color: var(--text-primary);
    font-size: var(--text-sm);
    background: transparent;
    border: none;
    cursor: pointer;
}

.topbar__menu-item:hover:not(:disabled) {
    background: var(--bg-hover);
}

.topbar__menu-item:disabled {
    color: var(--text-secondary);
    cursor: default;
}

.topbar__menu-item-policy {
    color: var(--text-secondary);
}

.topbar__menu-divider {
    height: 0.5px;
    background: var(--border);
    margin: var(--space-xs) 0;
}
</style>
