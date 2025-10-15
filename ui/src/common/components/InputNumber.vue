
<template>
  <div class="os-input-number" :class="$attrs['md-type'] && 'md-type'" tabindex="-1">
    <div class="p-float-label" :class="!$attrs.placeholder && 'no-label'" v-if="$attrs['md-type']">
      <InputNumber v-model="inputValue" :mode="'decimal'" :tabindex="tabOrder"
        :input-props="{disabled}" :minFractionDigits="maxFractionDigits"
        :maxFractionDigits="maxFractionDigits" @input="handleInput" />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <InputNumber v-model="inputValue" :placeholder="$attrs.placeholder" :tabindex="tabOrder"
        :mode="'decimal'" :minFractionDigits="maxFractionDigits" :maxFractionDigits="maxFractionDigits"
        :input-props="{disabled}" />
    </div>
    <div class="unit" v-if="unitText">
      <span>{{unitText}}</span>
    </div>
  </div>
</template>

<script>
import InputNumber from 'primevue/inputnumber';

export default {
  props: ['modelValue', 'maxFractionDigits', 'tabOrder', 'disabled', 'unit', 'form'],

  components: {
    InputNumber
  },

  data() {
    return {
    }
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue && +this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    unitText: function() {
      if (typeof this.unit == 'function') {
        return this.unit(this.form);
      }

      return this.unit;
    }
  },

  methods: {
    handleInput: function(event) {
      this.inputValue = event.value;
    },

    getDisplayValue: function() {
      return this.modelValue;
    }
  }
}
</script>

<style scoped>
.os-input-number .p-float-label:not(.no-label) {
  margin-top: 10px;
}

.os-input-number .p-float-label :deep(.p-inputtext) {
  border: 0px;
  border-bottom: 2px solid #ced4da;
  border-radius: 0px;
  padding: 2px 0px;
  box-shadow: none;
}

.os-input-number .p-float-label :deep(.p-inputtext:enabled:focus) {
  border-bottom-color: #007bff;
}

.os-input-number .p-float-label :deep(label) {
  left: 0rem;
  right: 0rem;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.os-input-number .p-float-label :deep(.p-inputtext:not(:enabled:focus) ~ label) {
  color: #999;
  opacity: 1;
}

.os-input-number :deep(.p-inputnumber) {
  width: 100%;
}

.os-input-number:has(.unit) {
  position: relative;
}

.os-input-number .unit {
  position: absolute;
  top: 0rem;
  bottom: 0rem;
  right: 0rem;
  padding: 0.5rem;
  background: #ddd;
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
  border: 1px solid #ced4da;
  border-left: 0;
}

.os-input-number.md-type .unit {
  background: transparent;
  border: 0;
  padding: 2px;
}
</style>
