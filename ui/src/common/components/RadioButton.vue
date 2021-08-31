
<template>
  <div class="os-radio-buttons">
    <div class="options-row" v-for="(optionsRow, rowIdx) of optionRows" :key="rowIdx">
      <div class="option" v-for="(option, optionIdx) of optionsRow" :key="optionIdx">
        <span class="p-field-radiobutton">
          <label>
            <RadioButton :name="name" :value="option.value" v-model="inputValue" />
            <span>{{option.caption}}</span>
          </label>
        </span>
      </div>
    </div>
  </div>
</template>

<script>

import RadioButton from 'primevue/radiobutton';

export default {
  props: ['name', 'options', 'modelValue', 'optionsPerRow'],

  emits: ['change', 'update:modelValue'],

  components: {
    RadioButton
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

.os-radio-buttons {
  display: table;
}

.os-radio-buttons .options-row {
  display: table-row;
}

.os-radio-buttons .options-row .option {
  display: table-cell;
}

.os-radio-buttons label {
  line-height: 1.30rem;
  margin-left: 0rem;
  cursor: pointer;
}

.os-radio-buttons label span {
  margin-left: 0.25rem;
  margin-right: 0.75rem;
}

</style>
