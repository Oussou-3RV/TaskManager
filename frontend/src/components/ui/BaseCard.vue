<template>
    <div class="card">
        <div v-if="title || $slots.header" class="card__header">
            <slot name="header">
                <h3 class="card__title">{{ title }}</h3>
            </slot>
        </div>
        <div class="card__body">
            <slot />
        </div>
        <div v-if="$slots.footer" class="card__footer">
            <slot name="footer" />
        </div>
    </div>
</template>

<script setup>
defineProps({
    title: { type: String, default: '' },
})
</script>

<style scoped>
.card {
    background: var(--bg-surface);
    border: 0.5px solid var(--border);
    border-radius: var(--radius-lg);
    display: flex;
    flex-direction: column;
}

.card__header {
    padding: var(--space-lg) var(--space-xl);
    border-bottom: 0.5px solid var(--border);
}

.card__title {
    margin: 0;
    font-size: var(--text-lg);
    font-weight: 600;
    color: var(--text-primary);
}

.card__body {
    padding: var(--space-xl);
    flex: 1;
}

.card__footer {
    padding: var(--space-lg) var(--space-xl);
    border-top: 0.5px solid var(--border);
}
</style>
