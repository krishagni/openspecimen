
<template>
  <div class="os-boolean-checkbox p-field-checkbox">
    <Checkbox :name="name" v-model="inputValue" :binary="true" :disabled="disabled" :tabindex="tabOrder" />
    <span class="inline-message" v-if="displayLabel"> {{ displayLabel }} </span>
    <slot></slot>
  </div>
</template>

<script>

import Checkbox from 'primevue/checkbox';

export default {
  props: ['name', 'modelValue', 'inlineLabel', 'inlineLabelCode', 'tabOrder', 'disabled'],

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
    },

    displayLabel: function() {
      if (this.inlineLabelCode) {
        return this.$osSvc.i18nSvc.msg(this.inlineLabelCode);
      } else {
        return this.inlineLabel;
      }
    }
  },

  methods: {
    getDisplayValue: function() {
      return this.$osSvc.i18nSvc.msg(
        (this.modelValue == true || this.modelValue == 1 || this.modelValue == 'true') ? 'common.yes' : 'common.no'
      );
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
