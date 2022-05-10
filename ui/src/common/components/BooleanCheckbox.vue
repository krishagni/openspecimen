
<template>
  <div class="p-field-checkbox">
    <Checkbox :name="name" v-model="inputValue" :binary="true" />
    <span class="inline-message" v-if="inlineLabel"> {{ inlineLabel }} </span>
    <slot></slot>
  </div>
</template>

<script>

import Checkbox from 'primevue/checkbox';

export default {
  props: ['name', 'modelValue', 'inlineLabel'],

  emits: ['change', 'update:modelValue'],

  components: {
    Checkbox
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue == 1 || this.modelValue == 'true' || this.modelValue == true;
      },

      set(value) {
        this.$emit('update:modelValue', value);
        this.$emit('change',  value);
      }
    }
  }
}
</script>

<style scoped>
.p-field-checkbox .inline-message {
  margin-left: 0.25rem;
  margin-right: 0.75rem;
}
</style>
