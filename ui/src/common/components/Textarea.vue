
<template>
  <div class="os-input-text">
    <div class="p-float-label" :class="!$attrs.placeholder && 'no-label'" v-if="$attrs['md-type']">
      <p-textarea v-model="inputValue" rows="2" :tabindex="tabOrder" v-bind="$attrs" />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <p-textarea v-model="inputValue" v-bind="$attrs" :tabindex="tabOrder" />
    </div>
  </div>
</template>

<script>
import Textarea from 'primevue/textarea';

export default {
  props: ['modelValue', 'tabOrder'],

  components: {
    'p-textarea': Textarea
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
      return this.modelValue;
    }
  }
}
</script>

<style scoped>
  .os-input-text .p-float-label:not(.no-label) {
    margin-top: 10px;
  }

  .os-input-text .p-float-label :deep(.p-inputtext) {
    border: 0px;
    border-bottom: 2px solid #ced4da;
    border-radius: 0px;
    padding: 2px 0px;
    box-shadow: none;
  }

  .os-input-text .p-float-label :deep(.p-inputtext:enabled:focus) {
    border-bottom-color: #007bff;
  }

  .os-input-text .p-float-label :deep(label) {
    left: 0rem;
  }

  .os-input-text .p-float-label :deep(.p-inputtext:not(:enabled:focus) ~ label) {
    color: #999;
    opacity: 1;
  }

  .os-input-text :deep(textarea) {
    resize: vertical;
    width: 100%;
  }
</style>
