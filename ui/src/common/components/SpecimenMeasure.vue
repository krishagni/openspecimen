<template>
  <div v-if="showInput">
    <div class="os-spmn-measure" :class="$attrs['md-type'] && 'md-type'">
      <os-input-number v-model="inputValue" :md-type="$attrs['md-type']" :maxFractionDigits="maxFractionDigits" />
      <div class="unit">
        <span>{{unit}}</span>
      </div>
    </div>
  </div>
  <div v-else>
    <span v-show="inputValue != undefined && inputValue != null">
      <span>{{inputValue}} {{unit}}</span>
    </span>
    <span v-show="inputValue == undefined || inputValue == null">
      <span>-</span>
    </span>
  </div>
</template>

<script>

import exprUtil from '@/common/services/ExpressionUtil.js';
import util from '@/common/services/Util.js';

export default {
  props: ['modelValue', 'maxFractionDigits', 'measure', 'entity', 'context', 'readOnly'],

  data() {
    return { 
    }
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue && +this.modelValue;
      },

      set(value) {
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
