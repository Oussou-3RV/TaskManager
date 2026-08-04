<template>
    <div class="tasks">
        <BaseCard title="Soumettre une tâche">
            <form class="tasks__form" @submit.prevent="handleSubmit">
                <BaseInput v-model="cpuRequired" label="CPU requis" type="number" placeholder="1.5" :error="errors.cpuRequired" />
                <BaseInput
                    v-model="memoryRequiredMb"
                    label="Mémoire requise (Mo)"
                    type="number"
                    placeholder="512"
                    :error="errors.memoryRequiredMb"
                />
                <BaseSelect v-model="priority" label="Priorité" :options="priorityOptions" />
                <BaseButton type="submit" :loading="submitting">Soumettre</BaseButton>
            </form>
        </BaseCard>

        <BaseCard title="Tâches">
            <div class="tasks__toolbar">
                <label class="tasks__toggle">
                    <input v-model="showAll" type="checkbox" @change="loadTasks" />
                    Toutes les tâches
                </label>
            </div>
            <BaseTable :columns="taskColumns" :rows="tasks" :loading="loading" empty-text="Aucune tâche">
                <template #priority="{ value }">
                    <BaseBadge :variant="priorityVariant(value)">{{ value }}</BaseBadge>
                </template>
                <template #status="{ value }">
                    <BaseBadge :variant="statusVariant(value)">{{ value }}</BaseBadge>
                </template>
                <template #createdAt="{ value }">{{ formatDate(value) }}</template>
                <template #actions="{ row }">
                    <BaseButton
                        v-if="row.status === 'PENDING' && row.submittedBy === auth.username"
                        variant="danger"
                        size="sm"
                        @click="cancelTask(row.id)"
                    >
                        Annuler
                    </BaseButton>
                </template>
            </BaseTable>
            <BasePagination v-model:page="page" :total-pages="totalPages" :total-elements="totalElements" @update:page="loadTasks" />
        </BaseCard>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api, { errorMessage } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import BasePagination from '@/components/ui/BasePagination.vue'

const auth = useAuthStore()
const notification = useNotificationStore()

const cpuRequired = ref('')
const memoryRequiredMb = ref('')
const priority = ref('normal')
const submitting = ref(false)
const errors = ref({ cpuRequired: '', memoryRequiredMb: '' })

const priorityOptions = [
    { value: 'low', label: 'Basse' },
    { value: 'normal', label: 'Normale' },
    { value: 'high', label: 'Haute' },
]

const tasks = ref([])
const loading = ref(false)
const showAll = ref(false)
const page = ref(0)
const totalPages = ref(1)
const totalElements = ref(0)

const taskColumns = [
    { key: 'id', label: 'ID' },
    { key: 'cpuRequired', label: 'CPU' },
    { key: 'memoryRequiredMb', label: 'Mémoire (Mo)' },
    { key: 'priority', label: 'Priorité' },
    { key: 'status', label: 'Statut' },
    { key: 'submittedBy', label: 'Soumis par' },
    { key: 'createdAt', label: 'Créée le' },
    { key: 'actions', label: '' },
]

function priorityVariant(value) {
    return { low: 'neutral', normal: 'accent', high: 'warning' }[value] || 'neutral'
}

function statusVariant(value) {
    return (
        {
            PENDING: 'warning',
            PLACED: 'success',
            REJECTED: 'danger',
            CANCELLED: 'neutral',
        }[value] || 'neutral'
    )
}

function formatDate(value) {
    return new Date(value).toLocaleString('fr-CA')
}

async function loadTasks() {
    loading.value = true
    try {
        const { data } = await api.get('/api/v1/tasks', {
            params: { all: showAll.value, page: page.value, size: 20 },
        })
        tasks.value = data.content
        totalPages.value = data.totalPages
        totalElements.value = data.totalElements
    } catch {
        notification.error('Impossible de charger les tâches.')
    } finally {
        loading.value = false
    }
}

function validate() {
    errors.value = { cpuRequired: '', memoryRequiredMb: '' }
    if (!(Number(cpuRequired.value) >= 0.1)) {
        errors.value.cpuRequired = 'Le CPU doit être supérieur à 0.1'
    }
    if (!(Number(memoryRequiredMb.value) >= 1)) {
        errors.value.memoryRequiredMb = 'La mémoire doit être supérieure à 0'
    }
    return !errors.value.cpuRequired && !errors.value.memoryRequiredMb
}

async function handleSubmit() {
    if (!validate()) return
    submitting.value = true
    try {
        const { data } = await api.post(
            '/api/v1/tasks',
            {
                cpuRequired: Number(cpuRequired.value),
                memoryRequiredMb: Number(memoryRequiredMb.value),
                priority: priority.value,
            },
            { headers: { 'Idempotency-Key': crypto.randomUUID() } },
        )
        notification[data.accepted ? 'success' : 'error'](
            data.accepted
                ? `Tâche placée sur ${data.selectedNodeId} — ${data.reason}`
                : `Tâche rejetée — ${data.reason}`,
        )
        cpuRequired.value = ''
        memoryRequiredMb.value = ''
        priority.value = 'normal'
        loadTasks()
    } catch (err) {
        notification.error(errorMessage(err, 'Échec de la soumission de la tâche.'))
    } finally {
        submitting.value = false
    }
}

async function cancelTask(id) {
    try {
        await api.delete(`/api/v1/tasks/${id}`)
        notification.success('Tâche annulée.')
        loadTasks()
    } catch (err) {
        notification.error(errorMessage(err, "Impossible d'annuler la tâche."))
    }
}

onMounted(loadTasks)
</script>

<style scoped>
.tasks {
    display: flex;
    flex-direction: column;
    gap: var(--space-2xl);
}

.tasks__form {
    display: flex;
    align-items: flex-end;
    gap: var(--space-lg);
    flex-wrap: wrap;
}

.tasks__toolbar {
    margin-bottom: var(--space-lg);
}

.tasks__toggle {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    font-size: var(--text-sm);
    color: var(--text-secondary);
}
</style>
