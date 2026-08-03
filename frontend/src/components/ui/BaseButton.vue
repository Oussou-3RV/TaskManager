<template>
    <button class="btn" :class="[`btn--${variant}`, `btn--${size}`]" :disabled="disabled || loading" @click="$emit('click', $event)">
        <span v-if="loading" class="btn__spinner" />
        <slot />
    </button>
</template>

<script setup>
defineProps({
    variant: { type: String, default: 'primary' }, // primary | secondary | danger | ghost
    size: { type: String, default: 'md' }, // sm | md | lg
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
})
defineEmits(['click'])
</script>

<style scoped>
.btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-sm);
    border-radius: var(--radius-md);
    border: 0.5px solid transparent;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 0.15s, border-color 0.15s, opacity 0.15s;
    white-space: nowrap;
}

.btn:disabled {
    cursor: not-allowed;
    opacity: 0.5;
}

.btn--sm {
    padding: 4px 10px;
    font-size: var(--text-sm);
}

.btn--md {
    padding: 8px 14px;
    font-size: var(--text-base);
}

.btn--lg {
    padding: 10px 18px;
    font-size: var(--text-lg);
}

.btn--primary {
    background: var(--accent);
    color: #fff;
}

.btn--primary:not(:disabled):hover {
    background: #3d5adf;
}

.btn--secondary {
    background: var(--bg-surface);
    border-color: var(--border);
    color: var(--text-primary);
}

.btn--secondary:not(:disabled):hover {
    background: var(--bg-hover);
}

.btn--danger {
    background: var(--danger-bg);
    border-color: var(--danger-border);
    color: var(--danger);
}

.btn--danger:not(:disabled):hover {
    background: var(--danger-border);
    color: var(--text-primary);
}

.btn--ghost {
    background: transparent;
    color: var(--text-secondary);
}

.btn--ghost:not(:disabled):hover {
    background: var(--bg-hover);
    color: var(--text-primary);
}

.btn__spinner {
    width: 12px;
    height: 12px;
    border: 2px solid currentColor;
    border-top-color: transparent;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}
</style>
