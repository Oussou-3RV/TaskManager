<template>
    <div class="audit">
        <BaseCard title="Journal d'audit">
            <div class="audit__filter">
                <BaseSelect
                    v-model="eventType"
                    label="Type d'événement"
                    placeholder="Tous"
                    :options="eventTypeOptions"
                    @update:modelValue="reload"
                />
            </div>
            <BaseTable :columns="columns" :rows="events" :loading="loading" empty-text="Aucun événement">
                <template #eventType="{ value }">
                    <BaseBadge :variant="eventVariant(value)">{{ value }}</BaseBadge>
                </template>
                <template #createdAt="{ value }">{{ formatDate(value) }}</template>
            </BaseTable>
            <BasePagination v-model:page="page" :total-pages="totalPages" :total-elements="totalElements" @update:page="reload" />
        </BaseCard>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'
import { useNotificationStore } from '@/stores/notification'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import BasePagination from '@/components/ui/BasePagination.vue'

const notification = useNotificationStore()

const events = ref([])
const loading = ref(false)
const eventType = ref('')
const page = ref(0)
const totalPages = ref(1)
const totalElements = ref(0)

const eventTypeOptions = [
    { value: 'TASK_SUBMITTED', label: 'Tâche soumise' },
    { value: 'TASK_PLACED', label: 'Tâche placée' },
    { value: 'TASK_REJECTED', label: 'Tâche rejetée' },
    { value: 'TASK_CANCELLED', label: 'Tâche annulée' },
]

const columns = [
    { key: 'eventType', label: 'Événement' },
    { key: 'actor', label: 'Acteur' },
    { key: 'entityType', label: 'Entité' },
    { key: 'entityId', label: 'ID entité' },
    { key: 'details', label: 'Détails' },
    { key: 'createdAt', label: 'Date' },
]

function eventVariant(value) {
    if (value.includes('REJECTED') || value.includes('CANCELLED')) return 'danger'
    if (value.includes('PLACED')) return 'success'
    return 'accent'
}

function formatDate(value) {
    return new Date(value).toLocaleString('fr-CA')
}

async function reload() {
    loading.value = true
    try {
        const { data } = await api.get('/api/v1/audit', {
            params: { eventType: eventType.value || undefined, page: page.value, size: 20 },
        })
        events.value = data.content
        totalPages.value = data.totalPages
        totalElements.value = data.totalElements
    } catch {
        notification.error("Impossible de charger le journal d'audit.")
    } finally {
        loading.value = false
    }
}

onMounted(reload)
</script>

<style scoped>
.audit {
    display: flex;
    flex-direction: column;
    gap: var(--space-2xl);
}

.audit__filter {
    max-width: 260px;
    margin-bottom: var(--space-lg);
}
</style>
