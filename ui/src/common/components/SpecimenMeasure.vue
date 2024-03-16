<template>
  <div v-if="showInput">
    <div class="os-spmn-measure" :class="$attrs['md-type'] && 'md-type'">
      <os-input-text v-model="userInput" :md-type="$attrs['md-type']" :tab-order="tabOrder" />
      <div class="unit">
        <span>{{unit}}</span>
      </div>
    </div>
  </div>
  <div v-else>
    <span v-show="displayValue != undefined && displayValue != null">
      <span>{{displayValue}} {{unit}}</span>
    </span>
    <span v-show="displayValue == undefined || displayValue == null">
      <span>-</span>
    </span>
  </div>
</template>

<script>

import exprUtil from '@/common/services/ExpressionUtil.js';
import util from '@/common/services/Util.js';

const NUMBER_RE = /^[0-9]*(\.[0-9]*)?(([e][+-]?)[0-9]*)?$/;

export default {
  props: ['modelValue', 'measure', 'entity', 'context', 'readOnly', 'tabOrder'],

  data() {
    return { 
      userInput: ''
    }
  },

  created() {
    if (this.inputValue >= 1e6 || (this.inputValue && this.inputValue <= 1e-5)) {
      this.userInput = this.inputValue.toExponential();
    } else {
      this.userInput = this.inputValue;
    }
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        if (value == '.') {
          value = '0.';
        } else if (value !== undefined && value !== null && String(value).trim().length > 0) {
          value = parseFloat(value);
        }

        this.$emit('update:modelValue', value);
      }
    },

    specimen: function() {
      if (!this.context || !this.entity) {
        return {};
      }

      return exprUtil.eval(this.context, this.entity);
    },

    unit: function() {
      return util.getSpecimenMeasureUnit(this.specimen, this.measure || 'quantity');
    },

    showInput: function() {
      return this.readOnly == null || this.readOnly == undefined ||
        util.isFalse(this.readOnly) ||
        util.isFalse(exprUtil.eval(this.context, this.readOnly + ' == true'));
    },

    displayValue: function() {
      const qty = this.inputValue;
      if (qty == undefined || qty == null || qty == '') {
        return null;
      } else if (qty >= 1e6 || (qty && qty <= 1e-5)) {
        return qty.toExponential();
      }

      return qty;
    }
  },

  watch: {
    inputValue: function(newVal, oldVal) {
      if (newVal == oldVal || newVal == parseFloat(this.userInput)) {
        return;
      }

      // if there is change in the model value outside of this component
      // then propagate to the UI input field.
      this.userInput = newVal;
    },

    userInput: function(newVal, oldVal) {
      // Propagate the changes in UI input to the model
      if ((!isNaN(newVal) && !newVal) || NUMBER_RE.test(newVal)) {
        this.inputValue = newVal;
        return;
      }

      setTimeout(() => this.userInput = oldVal);
    }
  },

  methods: {
    getDisplayValue: function() {
      let result = this.inputValue;
      if (!result || !this.unit) {
        return result;
      }

      return result + ' ' + this.unit;
    }
  }
}
</script>

<style scoped>

.os-spmn-measure {
  position: relative;
}

.os-spmn-measure .unit {
  position: absolute;
  top: 0rem;
  bottom: 0rem;
  right: 0rem;
  padding: 0.5rem;
  background: #ddd;
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
  border: 1px solid #ced4da;
  border-left: 0;
}

.os-spmn-measure.md-type .unit {
  background: transparent;
  border: 0;
  padding: 2px;
}
</style>
