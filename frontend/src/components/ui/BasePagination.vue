<template>
    <div class="pagination">
        <span class="pagination__info">
            Page {{ page + 1 }} sur {{ Math.max(totalPages, 1) }}
            <template v-if="totalElements !== null"> · {{ totalElements }} éléments</template>
        </span>
        <div class="pagination__controls">
            <BaseButton variant="secondary" size="sm" :disabled="page <= 0" @click="$emit('update:page', page - 1)">
                Précédent
            </BaseButton>
            <BaseButton
                variant="secondary"
                size="sm"
                :disabled="page + 1 >= totalPages"
                @click="$emit('update:page', page + 1)"
            >
                Suivant
            </BaseButton>
        </div>
    </div>
</template>

<script setup>
import BaseButton from './BaseButton.vue'

defineProps({
    page: { type: Number, required: true }, // 0-indexed
    totalPages: { type: Number, required: true },
    totalElements: { type: Number, default: null },
})
defineEmits(['update:page'])
</script>

<style scoped>
.pagination {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-lg);
    padding-top: var(--space-lg);
}

.pagination__info {
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

.pagination__controls {
    display: flex;
    gap: var(--space-sm);
}
</style>
