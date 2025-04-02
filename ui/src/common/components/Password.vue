
<template>
  <div class="os-input-password">
    <div class="p-float-label" :class="!$attrs.placeholder && 'no-label'" v-if="$attrs['md-type']">
      <p-password v-model="inputValue" :medium-regex="pattern" :strong-regex="pattern"
        :tabindex="tabOrder" toggleMask :feedback="false"/>
      <label>{{$attrs.placeholder}}</label>
    </div>
    <div v-else>
      <p-password v-model="inputValue" :placeholder="$attrs.placeholder" :medium-regex="pattern" :strong-regex="pattern"
        :tabindex="tabOrder" toggleMask :feedback="false"/>
    </div>
  </div>
</template>

<script>
import Password from 'primevue/password';

export default {
  props: ['modelValue', 'pattern', 'tabOrder'],

  components: {
    'p-password': Password
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
      return this.modelValue ? '*****' : null ;
    }
  }
}
</script>

<style scoped>
.os-input-password :deep(.p-password),
.os-input-password :deep(input) {
  width: 100%;
}

.os-input-password .p-float-label :deep(.p-inputtext) {
  border: 0px;
  border-bottom: 2px solid #ced4da;
  border-radius: 0px;
  padding: 2px 0px;
  box-shadow: none;
}

.os-input-password .p-float-label :deep(.p-inputtext:enabled:focus) {
  border-bottom-color: #007bff;
}

.os-input-password .p-float-label :deep(label) {
  left: 0rem;
  right: 0rem;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.os-input-password .p-float-label :deep(.p-inputtext:not(:enabled:focus) ~ label) {
  color: #999;
  opacity: 1;
}

.os-input-password .p-float-label:not(.no-label) {
  margin-top: 10px;
}
</style>
