<template>
    <div class="field">
        <label v-if="label" class="field__label">{{ label }}</label>
        <select class="field__select" :value="modelValue" @change="$emit('update:modelValue', $event.target.value)">
            <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
            <option v-for="option in options" :key="option.value" :value="option.value">
                {{ option.label }}
            </option>
        </select>
        <span v-if="error" class="field__error">{{ error }}</span>
    </div>
</template>

<script setup>
defineProps({
    label: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    options: { type: Array, default: () => [] }, // [{ value, label }]
    modelValue: { type: [String, Number], default: '' },
    error: { type: String, default: '' },
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.field {
    display: flex;
    flex-direction: column;
    gap: var(--space-xs);
}

.field__label {
    font-size: var(--text-sm);
    color: var(--text-secondary);
    font-weight: 500;
}

.field__select {
    background: var(--bg-app);
    border: 0.5px solid var(--border);
    border-radius: var(--radius-md);
    padding: 8px 12px;
    color: var(--text-primary);
    font-size: var(--text-base);
    font-family: inherit;
    transition: border-color 0.15s;
    outline: none;
}

.field__select:focus {
    border-color: var(--accent);
}

.field__error {
    font-size: var(--text-xs);
    color: var(--danger);
}
</style>
