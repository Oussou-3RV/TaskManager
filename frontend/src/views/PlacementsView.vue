<template>
    <div class="placements">
        <BaseCard title="Suivi des placements">
            <div class="placements__toolbar">
                <label class="placements__toggle">
                    <input v-model="showAll" type="checkbox" @change="loadHistory" />
                    Toutes les décisions
                </label>
            </div>
            <BaseTable :columns="columns" :rows="decisions" :loading="loading" empty-text="Aucune décision">
                <template #priority="{ value }">
                    <span class="placements__priority" :class="`placements__priority--${value}`">{{ value }}</span>
                </template>
                <template #selectedNodeId="{ value }">
                    <span v-if="value" class="placements__machine">{{ value }}</span>
                    <span v-else class="placements__dash">—</span>
                </template>
                <template #accepted="{ value }">
                    <BaseBadge :variant="value ? 'success' : 'danger'">{{ value ? 'PLACÉ' : 'REJETÉ' }}</BaseBadge>
                </template>
                <template #decidedAt="{ value }">{{ formatDate(value) }}</template>
            </BaseTable>
            <BasePagination
                v-model:page="page"
                :total-pages="totalPages"
                :total-elements="totalElements"
                @update:page="loadHistory"
            />
        </BaseCard>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api, { errorMessage } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import BasePagination from '@/components/ui/BasePagination.vue'

const auth = useAuthStore()
const notification = useNotificationStore()

const decisions = ref([])
const loading = ref(false)
// L'auditeur et l'admin n'ont pas de taches "a eux" : on leur montre tout par defaut.
const showAll = ref(auth.hasRole('AUDITOR', 'ADMIN'))
const page = ref(0)
const totalPages = ref(1)
const totalElements = ref(0)

const columns = [
    { key: 'taskId', label: 'Tâche' },
    { key: 'cpuRequired', label: 'CPU' },
    { key: 'memoryRequiredMb', label: 'RAM' },
    { key: 'priority', label: 'Priorité' },
    { key: 'policy', label: 'Politique' },
    { key: 'selectedNodeId', label: 'Machine' },
    { key: 'accepted', label: 'Résultat' },
    { key: 'reason', label: 'Justification' },
    { key: 'submittedBy', label: 'Soumis par' },
    { key: 'decidedAt', label: 'Décidé le' },
]

function formatDate(value) {
    return new Date(value).toLocaleString('fr-CA')
}

async function loadHistory() {
    loading.value = true
    try {
        const { data } = await api.get('/api/v1/placements', {
            params: { all: showAll.value, page: page.value, size: 20 },
        })
        decisions.value = data.content
        totalPages.value = data.totalPages
        totalElements.value = data.totalElements
    } catch (err) {
        notification.error(errorMessage(err, "Impossible de charger l'historique des placements."))
    } finally {
        loading.value = false
    }
}

onMounted(loadHistory)
</script>

<style scoped>
.placements__toolbar {
    margin-bottom: var(--space-lg);
}

.placements__toggle {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

.placements__priority {
    font-weight: 500;
}

.placements__priority--low {
    color: var(--text-secondary);
}

.placements__priority--normal {
    color: var(--warning);
}

.placements__priority--high {
    color: var(--danger);
}

.placements__machine {
    color: var(--accent);
}

.placements__dash {
    color: var(--text-secondary);
}
</style>
