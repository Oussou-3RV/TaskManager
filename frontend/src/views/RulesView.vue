<template>
    <div class="rules">
        <BaseCard v-if="auth.hasRole('ADMIN')" title="Nouvelle règle">
            <form class="rules__form" @submit.prevent="handleCreate">
                <BaseInput v-model="name" label="Nom" placeholder="ex: priorite-latence" />
                <BaseSelect v-model="policy" label="Politique" :options="policyOptions" />
                <BaseButton type="submit" :loading="creating">Créer</BaseButton>
            </form>
        </BaseCard>

        <BaseCard title="Règles de placement">
            <BaseTable :columns="columns" :rows="rules" :loading="loading" empty-text="Aucune règle">
                <template #isActive="{ value }">
                    <BaseBadge :variant="value ? 'success' : 'neutral'">{{ value ? 'Active' : 'Inactive' }}</BaseBadge>
                </template>
                <template #createdAt="{ value }">{{ formatDate(value) }}</template>
                <template #actions="{ row }">
                    <BaseButton v-if="auth.hasRole('ADMIN')" variant="secondary" size="sm" @click="toggleRule(row.id)">
                        {{ row.isActive ? 'Désactiver' : 'Activer' }}
                    </BaseButton>
                </template>
            </BaseTable>
        </BaseCard>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseTable from '@/components/ui/BaseTable.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'

const auth = useAuthStore()
const notification = useNotificationStore()

const rules = ref([])
const loading = ref(false)
const name = ref('')
const policy = ref('balanced')
const creating = ref(false)

const policyOptions = [
    { value: 'least_loaded', label: 'Least loaded' },
    { value: 'lowest_latency', label: 'Lowest latency' },
    { value: 'balanced', label: 'Balanced' },
]

const columns = [
    { key: 'name', label: 'Nom' },
    { key: 'policy', label: 'Politique' },
    { key: 'isActive', label: 'Statut' },
    { key: 'createdBy', label: 'Créée par' },
    { key: 'createdAt', label: 'Créée le' },
    { key: 'actions', label: '' },
]

function formatDate(value) {
    return new Date(value).toLocaleString('fr-CA')
}

async function loadRules() {
    loading.value = true
    try {
        const { data } = await api.get('/api/v1/rules')
        rules.value = data
    } catch {
        notification.error('Impossible de charger les règles.')
    } finally {
        loading.value = false
    }
}

async function handleCreate() {
    creating.value = true
    try {
        await api.post('/api/v1/rules', { name: name.value, policy: policy.value, isActive: true })
        notification.success('Règle créée.')
        name.value = ''
        policy.value = 'balanced'
        loadRules()
    } catch {
        notification.error('Échec de la création de la règle.')
    } finally {
        creating.value = false
    }
}

async function toggleRule(id) {
    try {
        await api.put(`/api/v1/rules/${id}/toggle`)
        loadRules()
    } catch {
        notification.error('Impossible de modifier la règle.')
    }
}

onMounted(loadRules)
</script>

<style scoped>
.rules {
    display: flex;
    flex-direction: column;
    gap: var(--space-2xl);
}

.rules__form {
    display: flex;
    align-items: flex-end;
    gap: var(--space-lg);
    flex-wrap: wrap;
}
</style>
