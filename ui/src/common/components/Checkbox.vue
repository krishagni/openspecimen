
<template>
  <div class="os-checkboxes">
    <div v-for="(optionsRow, rowIdx) of optionRows" :key="rowIdx">
      <span class="p-field-checkbox" v-for="(option, idx) of optionsRow" :key="idx">
        <Checkbox :name="name" :value="option.value" v-model="inputValue" />
        <label> <span>{{option.caption}}</span> </label>
      </span>
    </div>
  </div>
</template>

<script>

import Checkbox from 'primevue/checkbox';

export default {
  props: ['name', 'options', 'modelValue', 'optionsPerRow'],

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
    },

    optionRows: function() {
      let numOptions = this.optionsPerRow || 1;
      let result = [];
      for (let i = 0; i < this.options.length; i += numOptions) {
        result.push(this.options.slice(i, i + numOptions));
      }

      return result;
    }
  }
}

</script>

<style scoped>

.os-checkboxes {
  display: flex;
  flex-flow: row wrap;
}

.os-checkboxes label {
  line-height: 1.30rem;
  margin-left: 0rem;
  cursor: pointer;
}

.os-checkboxes label span {
  margin-left: 0.25rem;
  margin-right: 0.75rem;
}

</style>
