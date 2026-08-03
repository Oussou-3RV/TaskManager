<template>
    <div class="machine-card">
        <div class="machine-card__header">
            <span class="machine-card__dot" :class="machine.healthy ? 'machine-card__dot--ok' : 'machine-card__dot--down'" />
            <span class="machine-card__id">{{ machine.id }}</span>
        </div>
        <div class="machine-card__meta">{{ machine.level }} · {{ machine.latencyMs }}ms</div>
        <div class="machine-card__bar">
            <div class="machine-card__bar-fill" :class="{ 'machine-card__bar-fill--down': !machine.healthy }" :style="{ width: barWidth }" />
        </div>
        <div class="machine-card__footer">
            <template v-if="machine.healthy">
                <span>CPU libre</span>
                <span class="machine-card__value">{{ machine.cpuAvailable }}</span>
            </template>
            <span v-else class="machine-card__down-label">hors service</span>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    machine: { type: Object, required: true },
})

// Le backend n'expose pas de capacité CPU totale : on utilise une référence
// nominale par niveau pour représenter la charge relative dans la barre.
const LEVEL_CAPACITY = { edge: 4, fog: 8, cloud: 16 }

const barWidth = computed(() => {
    if (!props.machine.healthy) return '100%'
    const max = LEVEL_CAPACITY[props.machine.level] || 8
    const pct = Math.min(100, (Number(props.machine.cpuAvailable) / max) * 100)
    return `${pct}%`
})
</script>

<style scoped>
.machine-card {
    background: var(--bg-surface);
    border: 0.5px solid var(--border);
    border-radius: var(--radius-lg);
    padding: var(--space-lg);
    display: flex;
    flex-direction: column;
    gap: var(--space-sm);
}

.machine-card__header {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
}

.machine-card__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
}

.machine-card__dot--ok {
    background: var(--success);
}

.machine-card__dot--down {
    background: var(--danger);
}

.machine-card__id {
    font-weight: 600;
    color: var(--text-primary);
}

.machine-card__meta {
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

.machine-card__bar {
    height: 6px;
    border-radius: var(--radius-sm);
    background: var(--bg-hover);
    overflow: hidden;
}

.machine-card__bar-fill {
    height: 100%;
    background: var(--accent);
    border-radius: var(--radius-sm);
}

.machine-card__bar-fill--down {
    background: var(--border);
}

.machine-card__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

.machine-card__value {
    color: var(--text-primary);
    font-weight: 600;
}

.machine-card__down-label {
    color: var(--text-secondary);
}
</style>
