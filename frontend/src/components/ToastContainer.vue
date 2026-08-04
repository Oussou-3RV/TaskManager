<template>
    <div class="toasts">
        <div v-for="toast in notification.toasts" :key="toast.id" class="toast" :class="`toast--${toast.type}`">
            <span class="toast__message">{{ toast.message }}</span>
            <button class="toast__close" @click="notification.remove(toast.id)">✕</button>
        </div>
    </div>
</template>

<script setup>
import { useNotificationStore } from '@/stores/notification'

const notification = useNotificationStore()
</script>

<style scoped>
.toasts {
    position: fixed;
    top: var(--space-lg);
    right: var(--space-lg);
    z-index: 100;
    display: flex;
    flex-direction: column;
    gap: var(--space-sm);
    max-width: 360px;
}

.toast {
    display: flex;
    align-items: flex-start;
    gap: var(--space-md);
    padding: var(--space-md) var(--space-lg);
    border-radius: var(--radius-lg);
    border: 0.5px solid var(--border);
    background: var(--bg-surface);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.toast--success {
    border-color: var(--success-border);
    background: var(--success-bg);
}

.toast--danger {
    border-color: var(--danger-border);
    background: var(--danger-bg);
}

.toast--info {
    border-color: var(--accent-border);
    background: var(--accent-bg);
}

.toast__message {
    flex: 1;
    font-size: var(--text-sm);
    color: var(--text-primary);
}

.toast__close {
    background: transparent;
    border: none;
    color: var(--text-secondary);
    cursor: pointer;
    font-size: var(--text-sm);
    line-height: 1;
    padding: 0;
}

.toast__close:hover {
    color: var(--text-primary);
}
</style>
