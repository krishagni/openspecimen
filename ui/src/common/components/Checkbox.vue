
<template>
  <div>
    <span class="p-field-checkbox" v-for="(option, idx) of options" :key="idx">
      <Checkbox :name="name" :value="option.value" v-model="inputValue" />
      <label> <span>{{option.caption}}</span> </label>
    </span>
  </div>
</template>

<script>

import Checkbox from 'primevue/checkbox';

export default {
  props: ['name', 'options', 'modelValue'],

  emits: ['change', 'update:modelValue'],

  components: {
    Checkbox
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
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
</style>
