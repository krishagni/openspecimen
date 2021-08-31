
<template>
  <div class="p-field-checkbox">
    <Checkbox :name="name" v-model="inputValue" :binary="true" />
  </div>
</template>

<script>

import Checkbox from 'primevue/checkbox';

export default {
  props: ['name', 'modelValue'],

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
