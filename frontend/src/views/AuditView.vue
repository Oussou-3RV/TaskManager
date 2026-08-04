<template>
    <div class="audit">
        <BaseCard title="Journal d'audit">
            <div class="audit__filters">
                <BaseSelect v-model="eventType" label="Type d'événement" placeholder="Tous" :options="eventTypeOptions" />
                <BaseInput v-model="actor" label="Utilisateur" placeholder="ex: operateur" />
                <BaseInput v-model="machine" label="Machine" placeholder="ex: cloud-01" />
                <BaseSelect v-model="policy" label="Politique" placeholder="Toutes" :options="policyOptions" />
                <BaseInput v-model="from" label="Du" type="date" />
                <BaseInput v-model="to" label="Au" type="date" />
                <div class="audit__filter-actions">
                    <BaseButton variant="secondary" size="sm" @click="applyFilters">Filtrer</BaseButton>
                    <BaseButton variant="ghost" size="sm" @click="resetFilters">Réinitialiser</BaseButton>
                </div>
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
import api, { errorMessage } from '@/services/api'
import { useNotificationStore } from '@/stores/notification'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import BasePagination from '@/components/ui/BasePagination.vue'

const notification = useNotificationStore()

const events = ref([])
const loading = ref(false)
const eventType = ref('')
const actor = ref('')
const machine = ref('')
const policy = ref('')
const from = ref('')
const to = ref('')
const page = ref(0)
const totalPages = ref(1)
const totalElements = ref(0)

const eventTypeOptions = [
    { value: 'TASK_SUBMITTED', label: 'Tâche soumise' },
    { value: 'TASK_PLACED', label: 'Tâche placée' },
    { value: 'TASK_REJECTED', label: 'Tâche rejetée' },
    { value: 'TASK_CANCELLED', label: 'Tâche annulée' },
]

const policyOptions = [
    { value: 'least_loaded', label: 'Least loaded' },
    { value: 'lowest_latency', label: 'Lowest latency' },
    { value: 'balanced', label: 'Balanced' },
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

function applyFilters() {
    page.value = 0
    reload()
}

function resetFilters() {
    eventType.value = ''
    actor.value = ''
    machine.value = ''
    policy.value = ''
    from.value = ''
    to.value = ''
    applyFilters()
}

async function reload() {
    loading.value = true
    try {
        const { data } = await api.get('/api/v1/audit', {
            params: {
                eventType: eventType.value || undefined,
                actor: actor.value || undefined,
                machine: machine.value || undefined,
                policy: policy.value || undefined,
                from: from.value ? `${from.value}T00:00:00Z` : undefined,
                to: to.value ? `${to.value}T23:59:59Z` : undefined,
                page: page.value,
                size: 20,
            },
        })
        events.value = data.content
        totalPages.value = data.totalPages
        totalElements.value = data.totalElements
    } catch (err) {
        notification.error(errorMessage(err, "Impossible de charger le journal d'audit."))
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

.audit__filters {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    gap: var(--space-lg);
    margin-bottom: var(--space-lg);
}

.audit__filters > * {
    min-width: 160px;
}

.audit__filter-actions {
    display: flex;
    gap: var(--space-sm);
}
</style>
