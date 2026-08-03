<template>
    <div class="table-wrap">
        <table class="table">
            <thead>
                <tr>
                    <th v-for="col in columns" :key="col.key" :style="col.width ? { width: col.width } : undefined">
                        {{ col.label }}
                    </th>
                </tr>
            </thead>
            <tbody>
                <tr v-if="loading">
                    <td class="table__state" :colspan="columns.length">Chargement…</td>
                </tr>
                <tr v-else-if="!rows.length">
                    <td class="table__state" :colspan="columns.length">{{ emptyText }}</td>
                </tr>
                <tr v-for="(row, index) in rows" v-else :key="row.id ?? index">
                    <td v-for="col in columns" :key="col.key">
                        <slot :name="col.key" :row="row" :value="row[col.key]">
                            {{ row[col.key] }}
                        </slot>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script setup>
defineProps({
    columns: { type: Array, required: true }, // [{ key, label, width? }]
    rows: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    emptyText: { type: String, default: 'Aucune donnée' },
})
</script>

<style scoped>
.table-wrap {
    overflow-x: auto;
    border: 0.5px solid var(--border);
    border-radius: var(--radius-lg);
}

.table {
    width: 100%;
    border-collapse: collapse;
    font-size: var(--text-base);
}

.table th {
    text-align: left;
    padding: var(--space-md) var(--space-lg);
    font-size: var(--text-xs);
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    color: var(--text-secondary);
    background: var(--bg-surface);
    border-bottom: 0.5px solid var(--border);
    white-space: nowrap;
}

.table td {
    padding: var(--space-md) var(--space-lg);
    border-bottom: 0.5px solid var(--border-light);
    color: var(--text-primary);
}

.table tbody tr:last-child td {
    border-bottom: none;
}

.table tbody tr:hover td {
    background: var(--bg-hover);
}

.table__state {
    text-align: center;
    padding: var(--space-2xl);
    color: var(--text-secondary);
}

.table__state:hover {
    background: transparent !important;
}
</style>
