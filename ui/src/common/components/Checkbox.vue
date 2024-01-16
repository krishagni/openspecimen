
<template>
  <div class="os-checkboxes">
    <div class="options-row" v-for="(optionsRow, rowIdx) of optionRows" :key="rowIdx">
      <div class="option" v-for="(option, idx) of optionsRow" :key="idx">
        <span class="p-field-checkbox">
          <div>
            <Checkbox :id="name + '.' + rowIdx + '.' + idx" :name="name" :value="option.value" v-model="inputValue"
              :tabindex="tabOrder" :disabled="disabled" />
            <label :for="name + '.' + rowIdx + '.' + idx">
              <span>{{option.caption}}</span>
            </label>
          </div>
        </span>
      </div>
    </div>
  </div>
</template>

<script>

import Checkbox from 'primevue/checkbox';

export default {
  props: ['name', 'options', 'modelValue', 'optionsPerRow', 'tabOrder', 'disabled'],

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
  },

  methods: {
    getDisplayValue: function() {
      if (!this.modelValue || !(this.modelValue instanceof Array)) {
        return null;
      }

      let selectedValues = [];
      for (let optionRow of this.optionRows) {
        for (let option of optionRow) {
          if (this.modelValue.indexOf(option.value) >= 0) {
            selectedValues.push(option.caption);
          }
        }
      }

      return selectedValues.join(', ');
    }
  }
}

</script>

<style scoped>

.os-checkboxes {
  display: table;
}

.os-checkboxes :deep(.p-field-checkbox) {
  margin-bottom: 0rem;
}

.os-checkboxes .options-row {
  display: table-row;
}

.os-checkboxes .options-row .option {
  display: table-cell;
}

.os-checkboxes .options-row:not(:last-child) :deep(.p-field-checkbox) {
  margin-bottom: 1rem;
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
