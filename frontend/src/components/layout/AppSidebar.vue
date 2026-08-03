<template>
    <aside class="sidebar">
        <div class="sidebar__brand">
            <span class="sidebar__brand-name">Projet D4</span>
            <span class="sidebar__brand-subtitle">Console de pilotage</span>
        </div>
        <nav class="sidebar__nav">
            <RouterLink
                v-for="item in visibleItems"
                :key="item.to"
                :to="item.to"
                class="sidebar__link"
                active-class="sidebar__link--active"
            >
                <span class="sidebar__icon">{{ item.icon }}</span>
                <span>{{ item.label }}</span>
            </RouterLink>
        </nav>
        <div class="sidebar__user">
            <span class="sidebar__avatar">{{ initials }}</span>
            <div class="sidebar__user-info">
                <span class="sidebar__username">{{ auth.username }}</span>
                <span class="sidebar__role">{{ auth.role }}</span>
            </div>
        </div>
    </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const navItems = [
    { to: '/', label: 'Dashboard', icon: '◎', roles: ['OPERATOR', 'ADMIN', 'AUDITOR'] },
    { to: '/tasks', label: 'Soumettre', icon: '➤', roles: ['OPERATOR', 'ADMIN'] },
    { to: '/machines', label: 'Machines', icon: '▦', roles: ['OPERATOR', 'ADMIN', 'AUDITOR'] },
    { to: '/rules', label: 'Règles', icon: '⚙', roles: ['OPERATOR', 'ADMIN', 'AUDITOR'] },
    { to: '/audit', label: 'Audit', icon: '▤', roles: ['AUDITOR', 'ADMIN'] },
]

const visibleItems = computed(() => navItems.filter((item) => auth.hasRole(...item.roles)))

const initials = computed(() => (auth.username || '').slice(0, 2).toUpperCase())
</script>

<style scoped>
.sidebar {
    width: var(--sidebar-width);
    height: 100%;
    background: var(--bg-surface);
    border-right: 0.5px solid var(--border);
    display: flex;
    flex-direction: column;
}

.sidebar__brand {
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: var(--space-lg);
    border-bottom: 0.5px solid var(--border);
}

.sidebar__brand-name {
    font-weight: 700;
    font-size: var(--text-lg);
    color: var(--text-primary);
}

.sidebar__brand-subtitle {
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

.sidebar__nav {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: var(--space-lg) var(--space-md);
    gap: var(--space-xs);
}

.sidebar__link {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    padding: var(--space-sm) var(--space-md);
    border-radius: var(--radius-md);
    color: var(--text-secondary);
    font-size: var(--text-base);
    transition: background-color 0.15s, color 0.15s;
}

.sidebar__link:hover {
    background: var(--bg-hover);
    color: var(--text-primary);
}

.sidebar__link--active {
    background: var(--accent-bg);
    color: var(--accent);
}

.sidebar__icon {
    width: 16px;
    text-align: center;
}

.sidebar__user {
    display: flex;
    align-items: center;
    gap: var(--space-md);
    margin: var(--space-md);
    padding: var(--space-md);
    background: var(--bg-hover);
    border-radius: var(--radius-lg);
}

.sidebar__avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: var(--accent);
    color: #fff;
    font-size: var(--text-sm);
    font-weight: 600;
    flex-shrink: 0;
}

.sidebar__user-info {
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.sidebar__username {
    font-size: var(--text-sm);
    font-weight: 600;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.sidebar__role {
    font-size: var(--text-xs);
    color: var(--text-secondary);
}
</style>
