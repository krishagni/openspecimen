
<template>
  <div class="os-input-number">
    <div class="p-float-label" :class="!$attrs.placeholder && 'no-label'" v-if="$attrs['md-type']">
      <InputNumber v-model="inputValue" :mode="'decimal'"
        :minFractionDigits="maxFractionDigits" :maxFractionDigits="maxFractionDigits" @input="handleInput" />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <InputNumber v-model="inputValue" :placeholder="$attrs.placeholder"
        :mode="'decimal'" :minFractionDigits="maxFractionDigits" :maxFractionDigits="maxFractionDigits" />
    </div>
  </div>
</template>

<script>
import InputNumber from 'primevue/inputnumber';

export default {
  props: ['modelValue', 'maxFractionDigits'],

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
    }
  },

  methods: {
    handleInput: function(event) {
      this.inputValue = event.value;
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
}

.os-input-number .p-float-label :deep(.p-inputtext:not(:enabled:focus) ~ label) {
  color: #999;
  opacity: 1;
}

.os-input-number :deep(.p-inputnumber) {
  width: 100%;
}
</style>
