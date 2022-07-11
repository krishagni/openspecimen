
<template>
  <div class="os-input-password">
    <div class="p-float-label" v-if="$attrs['md-type']">
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
  }
}
</script>

<style scoped>
  .os-input-password :deep(.p-password),
  .os-input-password :deep(input) {
    width: 100%;
  }
</style>
