<template>
    <div class="dashboard">
        <div v-if="auth.hasRole('OPERATOR', 'ADMIN')" class="dashboard__stats">
            <BaseCard title="Tâches soumises">
                <p class="dashboard__stat-value">{{ submittedToday }}</p>
                <p class="dashboard__stat-label">aujourd'hui</p>
            </BaseCard>
            <BaseCard title="Placées">
                <p class="dashboard__stat-value dashboard__stat-value--success">{{ placedCount }}</p>
                <p class="dashboard__stat-label">{{ successRate }}% succès</p>
            </BaseCard>
            <BaseCard title="Rejetées">
                <p class="dashboard__stat-value dashboard__stat-value--danger">{{ rejectedCount }}</p>
                <p class="dashboard__stat-label">ressources insuffisantes</p>
            </BaseCard>
            <BaseCard title="Machines actives">
                <p class="dashboard__stat-value">{{ healthyMachines }} / {{ machines.length }}</p>
                <p class="dashboard__stat-label">{{ machines.length - healthyMachines }} hors service</p>
            </BaseCard>
        </div>
        <div v-else class="dashboard__stats">
            <BaseCard title="Machines actives">
                <p class="dashboard__stat-value">{{ healthyMachines }} / {{ machines.length }}</p>
                <p class="dashboard__stat-label">{{ machines.length - healthyMachines }} hors service</p>
            </BaseCard>
        </div>

        <BaseCard v-if="auth.hasRole('OPERATOR', 'ADMIN')" title="Tâches récentes">
            <BaseTable :columns="taskColumns" :rows="recentTasks" :loading="loadingTasks" empty-text="Aucune tâche">
                <template #priority="{ value }">
                    <span class="dashboard__priority" :class="`dashboard__priority--${value}`">{{ value }}</span>
                </template>
                <template #machine="{ row }">
                    <span v-if="machineByTask[row.id]" class="dashboard__machine">{{ machineByTask[row.id] }}</span>
                    <span v-else class="dashboard__dash">—</span>
                </template>
                <template #status="{ value }">
                    <span class="dashboard__status">
                        <span class="dashboard__status-dot" :class="`dashboard__status-dot--${statusVariant(value)}`" />
                        {{ value }}
                    </span>
                </template>
            </BaseTable>
        </BaseCard>

        <BaseCard title="État des machines">
            <p v-if="loadingMachines" class="dashboard__state">Chargement…</p>
            <p v-else-if="!machines.length" class="dashboard__state">Aucune machine</p>
            <div v-else class="dashboard__machine-grid">
                <MachineCard v-for="machine in machines" :key="machine.id" :machine="machine" />
            </div>
        </BaseCard>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api, { errorMessage } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import MachineCard from '@/components/MachineCard.vue'

const auth = useAuthStore()
const notification = useNotificationStore()

const machines = ref([])
const tasks = ref([])
const machineByTask = ref({})
const loadingMachines = ref(false)
const loadingTasks = ref(false)

const taskColumns = [
    { key: 'id', label: 'ID' },
    { key: 'cpuRequired', label: 'CPU' },
    { key: 'memoryRequiredMb', label: 'RAM' },
    { key: 'priority', label: 'Priorité' },
    { key: 'machine', label: 'Machine' },
    { key: 'status', label: 'Statut' },
]

const recentTasks = computed(() =>
    [...tasks.value].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 5),
)

const submittedToday = computed(
    () => tasks.value.filter((t) => new Date(t.createdAt).toDateString() === new Date().toDateString()).length,
)
const placedCount = computed(() => tasks.value.filter((t) => t.status === 'PLACED').length)
const rejectedCount = computed(() => tasks.value.filter((t) => t.status === 'REJECTED').length)
const successRate = computed(() =>
    tasks.value.length ? Math.round((placedCount.value / tasks.value.length) * 1000) / 10 : 0,
)

const healthyMachines = computed(() => machines.value.filter((m) => m.healthy).length)

function statusVariant(status) {
    return { PLACED: 'success', PENDING: 'warning', REJECTED: 'danger', CANCELLED: 'neutral' }[status] || 'neutral'
}

async function loadMachines() {
    loadingMachines.value = true
    try {
        const { data } = await api.get('/api/v1/machines')
        machines.value = data
    } catch (err) {
        notification.error(errorMessage(err, 'Impossible de charger les machines.'))
    } finally {
        loadingMachines.value = false
    }
}

async function loadTasks() {
    if (!auth.hasRole('OPERATOR', 'ADMIN')) return
    loadingTasks.value = true
    try {
        const { data } = await api.get('/api/v1/tasks', { params: { all: true, size: 100 } })
        tasks.value = data.content
    } catch (err) {
        notification.error(errorMessage(err, 'Impossible de charger les tâches.'))
    } finally {
        loadingTasks.value = false
    }
}

async function loadTaskMachines() {
    // Le nœud assigné n'est exposé que via le journal d'audit (accès AUDITOR/ADMIN
    // uniquement) : on tente l'appel et on dégrade silencieusement sinon.
    try {
        const { data } = await api.get('/api/v1/audit', { params: { eventType: 'TASK_PLACED', size: 100 } })
        const map = {}
        for (const event of data.content) {
            try {
                map[event.entityId] = JSON.parse(event.details).machine
            } catch {
                // détails non parsables, on ignore cet événement
            }
        }
        machineByTask.value = map
    } catch {
        machineByTask.value = {}
    }
}

onMounted(() => {
    loadMachines()
    loadTasks().then(loadTaskMachines)
})
</script>

<style scoped>
.dashboard {
    display: flex;
    flex-direction: column;
    gap: var(--space-2xl);
}

.dashboard__stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: var(--space-lg);
}

.dashboard__stat-value {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    color: var(--text-primary);
}

.dashboard__stat-value--success {
    color: var(--success);
}

.dashboard__stat-value--danger {
    color: var(--danger);
}

.dashboard__stat-label {
    margin: var(--space-xs) 0 0;
    font-size: var(--text-sm);
    color: var(--text-secondary);
}

.dashboard__state {
    text-align: center;
    color: var(--text-secondary);
    padding: var(--space-2xl);
}

.dashboard__machine-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: var(--space-lg);
}

.dashboard__priority {
    font-weight: 500;
}

.dashboard__priority--low {
    color: var(--text-secondary);
}

.dashboard__priority--normal {
    color: var(--warning);
}

.dashboard__priority--high {
    color: var(--danger);
}

.dashboard__machine {
    color: var(--accent);
}

.dashboard__dash {
    color: var(--text-secondary);
}

.dashboard__status {
    display: inline-flex;
    align-items: center;
    gap: var(--space-sm);
}

.dashboard__status-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
}

.dashboard__status-dot--success {
    background: var(--success);
}

.dashboard__status-dot--warning {
    background: var(--warning);
}

.dashboard__status-dot--danger {
    background: var(--danger);
}

.dashboard__status-dot--neutral {
    background: var(--text-secondary);
}
</style>
