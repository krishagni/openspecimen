<template>
  <p-select-button class="os-select-button" v-model="inputValue" :tabindex="tabOrder"
    :options="options" :option-label="optionLabel" :option-value="optionValue"
    :disabled="$attrs.disabled" />
</template>

<script>
import SelectButton from 'primevue/selectbutton';

export default {
  props: ['modelValue', 'tabOrder', 'options', 'option-label', 'option-value'],

  components: {
    'p-select-button': SelectButton
  },

  data() {
    return {
    }
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  methods: {
    getDisplayValue: function() {
      if (this.modelValue == null || this.modelValue == undefined) {
        return '-';
      }

      for (let option of this.options) {
        if (option[this.optionValue] == this.modelValue) {
          return option[this.optionLabel];
        }
      }

      return this.modelValue;
    }
  }
}
</script>

<style scoped>
.os-select-button :deep(.p-button) {
  background: #fff;
  color: inherit;
}

.os-select-button :deep(.p-button.p-highlight) {
  background: #337ab7;
  border-color: #2e6da4;
  color: #fff;
}

.os-select-button :deep(.p-button:first-child) {
  border-top-left-radius: 1.25rem;
  border-bottom-left-radius: 1.25rem;
}

.os-select-button :deep(.p-button:last-child) {
  border-top-right-radius: 1.25rem;
  border-bottom-right-radius: 1.25rem;
}
</style>
