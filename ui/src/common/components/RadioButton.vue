
<template>
  <div class="os-radio-wrapper">
    <div class="os-radio-buttons">
      <div :class="rowClass" v-for="(optionsRow, rowIdx) of optionRows" :key="rowIdx">
        <div class="option" v-for="(option, optionIdx) of optionsRow" :key="optionIdx">
          <span class="p-field-radiobutton">
            <label>
              <RadioButton :name="name" :value="option.value" v-model="inputValue"
                :tabindex="tabOrder" :disabled="disabled" />
              <span>{{option.displayLabel}}</span>
            </label>
            <os-icon name="question-circle" v-os-tooltip="$t(option.tooltipCode)" v-if="option.tooltipCode" />
          </span>
        </div>
      </div>
    </div>
    <os-button size="small" left-icon="times" v-if="clearOption" @click="inputValue = null" />
  </div>
</template>

<script>

import RadioButton from 'primevue/radiobutton';

export default {
  props: [
    'name', 'options', 'modelValue',
    'optionsPerRow', 'context', 'tabOrder',
    'disabled', 'clearOption'
  ],

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

    optionsList: function() {
      let result = [];
      if (this.options instanceof Array) {
        result = this.options;
      } else if (typeof this.options == 'function') {
        result = this.options(this.context);
      }

      return (result || []).map(
        option => {
          if (typeof option == 'string') {
            return {caption: option, value: option};
          } else {
            return option;
          }
        }
      );
    },

    optionRows: function() {
      const optionsList = this.optionsList;
      const numOptions = this.optionsPerRow || 1;
      const result = [];

      for (let i = 0; i < optionsList.length; i += numOptions) {
        result.push(optionsList.slice(i, i + numOptions));
      }

      for (let row of result) {
        for (let option of row) {
          if (option.captionCode) {
            option.displayLabel = this.$t(option.captionCode);
          } else {
            option.displayLabel = option.caption;
          }
        }
      }

      return result;
    },

    rowClass: function() {
      return this.optionRows.length > 1 ? ['options-row'] : ['options-row', 'compact'];
    }
  },

  methods: {
    getDisplayValue: function() {
      if (!this.modelValue) {
        return null;
      }

      for (let optionRow of this.optionRows) {
        for (let option of optionRow) {
          if (option.value == this.modelValue) {
            return option.displayLabel;
          }
        }
      }

      return null;
    }
  }
}

</script>

<style scoped>

.os-radio-buttons {
  display: table;
}

.os-radio-buttons :deep(.p-field-radiobutton) {
  margin-bottom: 0rem;
}

.os-radio-buttons .options-row {
  display: table-row;
}

.os-radio-buttons .options-row.compact {
  display: block;
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

.os-radio-buttons .options-row.compact label span {
  margin-right: 1rem;
}

.os-radio-buttons .options-row:not(:last-child) :deep(.p-field-radiobutton) {
  margin-bottom: 1rem;
}

.os-radio-wrapper {
  display: flex;
  align-items: center;
}

.os-radio-wrapper .os-radio-buttons {
  flex: 1;
}
</style>
