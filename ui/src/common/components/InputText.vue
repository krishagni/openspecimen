
<template>
  <div class="os-input-text">
    <div class="p-float-label" :class="!$attrs.placeholder && 'no-label'" v-if="$attrs['md-type']">
      <p-input-text ref="inputEl" type="text" v-model="inputValue" :tabindex="tabOrder"  :disabled="$attrs.disabled" />
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <p-input-text ref="inputEl" type="text" v-model="inputValue" :tabindex="tabOrder"
        :placeholder="$attrs.placeholder" :disabled="$attrs.disabled" />
    </div>
  </div>
</template>

<script>
import InputText from 'primevue/inputtext';

export default {
  props: ['modelValue', 'tabOrder', 'debounce'],

  emits: ['change', 'update:modelValue'],

  components: {
    'p-input-text': InputText
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
        if (this.debounceTimer) {
          clearTimeout(this.debounceTimer);
        }

        this.debounceTimer = setTimeout(() => this.$emit('change', value), this.debounce);
      }
    }
  },

  methods: {
    getDisplayValue: function() {
      return this.modelValue;
    },

    focus: function() {
      this.$refs.inputEl.$el.focus();
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
    right: 0rem;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .os-input-text .p-float-label :deep(.p-inputtext:not(:enabled:focus) ~ label) {
    color: #999;
    opacity: 1;
  }

  .os-input-text input {
    width: 100%;
  }
</style>
