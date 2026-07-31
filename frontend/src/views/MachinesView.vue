<template>
    <div class="machines">
        <BaseCard title="Machines">
            <label class="machines__toggle">
                <input v-model="healthyOnly" type="checkbox" @change="loadMachines" />
                Machines saines seulement
            </label>
            <p v-if="loading" class="machines__state">Chargement…</p>
            <p v-else-if="!machines.length" class="machines__state">Aucune machine</p>
            <div v-else class="machines__grid">
                <MachineCard v-for="machine in machines" :key="machine.id" :machine="machine" />
            </div>
        </BaseCard>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/services/api'
import { useNotificationStore } from '@/stores/notification'
import BaseCard from '@/components/ui/BaseCard.vue'
import MachineCard from '@/components/MachineCard.vue'

const notification = useNotificationStore()

const machines = ref([])
const loading = ref(false)
const healthyOnly = ref(false)

async function loadMachines() {
    loading.value = true
    try {
        const { data } = await api.get('/api/v1/machines', {
            params: healthyOnly.value ? { healthyOnly: true } : {},
        })
        machines.value = data
    } catch {
        notification.error('Impossible de charger les machines.')
    } finally {
        loading.value = false
    }
}

onMounted(loadMachines)
</script>

<style scoped>
.machines__toggle {
    display: flex;
    align-items: center;
    gap: var(--space-sm);
    font-size: var(--text-sm);
    color: var(--text-secondary);
    margin-bottom: var(--space-lg);
}

.machines__state {
    text-align: center;
    color: var(--text-secondary);
    padding: var(--space-2xl);
}

.machines__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: var(--space-lg);
}
</style>
