
<template>
  <div class="os-input-number">
    <div class="p-float-label" v-if="$attrs['md-type']">
      <InputNumber v-model="inputValue" :mode="decimal"
        :minFractionDigits="maxFractionDigits" :maxFractionDigits="maxFractionDigits" />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <InputNumber v-model="inputValue" :placeholder="$attrs.placeholder"
        :mode="decimal" :minFractionDigits="maxFractionDigits" :maxFractionDigits="maxFractionDigits" />
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
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    }
  },

  methods: {
  }
}
</script>

<style scoped>
.os-input-number .p-float-label {
  margin-top: 10px;
}

.os-input-number .p-float-label :deep(.p-inputnumber) {
  border: 0px;
  border-bottom: 2px solid #ced4da;
  border-radius: 0px;
  padding: 2px 0px;
  box-shadow: none;
}

.os-input-number .p-float-label :deep(.p-inputnumber:enabled:focus) {
  border-bottom-color: #007bff;
}

.os-input-number .p-float-label :deep(label) {
  left: 0rem;
}

.os-input-number .p-float-label :deep(.p-inputnumber:not(:enabled:focus) ~ label) {
  color: #999;
  opacity: 1;
}

.os-input-number :deep(.p-inputnumber) {
  width: 100%;
}
</style>
