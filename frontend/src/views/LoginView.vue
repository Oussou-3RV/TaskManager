<template>
    <div class="login">
        <BaseCard title="Connexion" class="login__card">
            <form class="login__form" @submit.prevent="handleSubmit">
                <BaseInput v-model="username" label="Nom d'utilisateur" placeholder="operateur1" />
                <BaseInput v-model="password" label="Mot de passe" type="password" placeholder="••••••••" />
                <p v-if="errorMessage" class="login__error">{{ errorMessage }}</p>
                <BaseButton type="submit" :loading="loading" :disabled="!username || !password">
                    Se connecter
                </BaseButton>
            </form>
        </BaseCard>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function handleSubmit() {
    loading.value = true
    errorMessage.value = ''
    try {
        await auth.login({ username: username.value, password: password.value })
        router.push(route.query.redirect || '/')
    } catch {
        errorMessage.value = "Nom d'utilisateur ou mot de passe incorrect."
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.login {
    height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-app);
}

.login__card {
    width: 360px;
}

.login__form {
    display: flex;
    flex-direction: column;
    gap: var(--space-lg);
}

.login__error {
    margin: 0;
    font-size: var(--text-sm);
    color: var(--danger);
}
</style>
