<template>
    <span class="badge" :class="`badge--${variant}`">
        <slot />
    </span>
</template>

<script setup>
defineProps({
    variant: { type: String, default: 'neutral' }, // neutral | accent | success | warning | danger
})
</script>

<style scoped>
.badge {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: var(--radius-sm);
    font-size: var(--text-xs);
    font-weight: 500;
    border: 0.5px solid transparent;
    line-height: 1.6;
}

.badge--neutral {
    background: var(--bg-hover);
    border-color: var(--border);
    color: var(--text-secondary);
}

.badge--accent {
    background: var(--accent-bg);
    border-color: var(--accent-border);
    color: var(--accent);
}

.badge--success {
    background: var(--success-bg);
    border-color: var(--success-border);
    color: var(--success);
}

.badge--warning {
    background: var(--warning-bg);
    border-color: var(--warning-border);
    color: var(--warning);
}

.badge--danger {
    background: var(--danger-bg);
    border-color: var(--danger-border);
    color: var(--danger);
}
</style>
