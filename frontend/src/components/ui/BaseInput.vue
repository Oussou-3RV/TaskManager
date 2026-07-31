<template>
    <div class="field">
        <label v-if="label" class="field__label">{{ label }}</label>
        <input class="field__input" :type="type" :placeholder="placeholder" :value="modelValue"
            @input="$emit('update:modelValue', $event.target.value)" />
        <span v-if="error" class="field__error">{{ error }}</span>
    </div>
</template>

<script setup>
defineProps({
    label: { type: String, default: '' },
    type: { type: String, default: 'text' },
    placeholder: { type: String, default: '' },
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

.field__input {
    background: var(--bg-app);
    border: 0.5px solid var(--border);
    border-radius: var(--radius-md);
    padding: 8px 12px;
    color: var(--text-primary);
    font-size: var(--text-base);
    transition: border-color 0.15s;
    outline: none;
}

.field__input:focus {
    border-color: var(--accent);
}

.field__input::placeholder {
    color: var(--text-secondary);
}

.field__error {
    font-size: var(--text-xs);
    color: var(--danger);
}
</style>