<template>
    <div class="layout">
        <AppSidebar />
        <div class="layout__main">
            <AppTopbar :title="pageTitle" />
            <main class="layout__content">
                <RouterView />
            </main>
        </div>
    </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from './AppSidebar.vue'
import AppTopbar from './AppTopbar.vue'

const route = useRoute()
const pageTitle = computed(() => route.meta.title || '')
</script>

<style scoped>
.layout {
    display: flex;
    height: 100vh;
}

.layout__main {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.layout__content {
    flex: 1;
    overflow-y: auto;
    padding: var(--space-2xl);
}
</style>
