
<template>
  <ul class="range-facet">
    <li class="input" v-if="facet.type == 'INTEGER' || facet.type == 'FLOAT'">
      <os-input-number :md-type="true" :placeholder="$t('queries.min_value')" v-model="minValue" v-if="showMinLimit" />

      <os-input-number :md-type="true" :placeholder="$t('queries.max_value')" v-model="maxValue" v-if="showMaxLimit" />

      <os-button success :label="$t('common.buttons.go')" @click="addLimit" />
    </li>

    <li class="input" v-if="facet.type == 'DATE'">
      <os-date-picker :md-type="true" :placeholder="$t('queries.min_value')" v-model="minValue" v-if="showMinLimit" />

      <os-date-picker :md-type="true" :placeholder="$t('queries.max_value')" v-model="maxValue" v-if="showMaxLimit" />

      <os-button success :label="$t('common.buttons.go')" @click="addDateLimit" />
    </li>

    <li v-for="(item, idx) of list" :key="idx">
      <os-boolean-checkbox v-model="item.selected" @change="toggleSelection(item)" />

      <span v-if="(item.minValue || item.minValue === 0) && (item.maxValue || item.maxValue === 0)">
        <span>{{item.minValue}} - {{item.maxValue}}</span>
      </span>
      <span v-else-if="item.minValue || item.minValue === 0">
        <span v-if="facet.op == 'GT'"> &gt; {{item.minValue}} </span>
        <span v-else> &gt;= {{item.minValue}} </span>
      </span>
      <span v-else-if="item.maxValue || item.maxValue === 0">
        <span v-if="facet.op == 'LT'"> &lt; {{item.maxValue}} </span>
        <span v-else> &lt;= {{item.maxValue}} </span>
      </span>
    </li>
  </ul>
</template>

<script>
import util from '@/common/services/Util.js';

export default {
  props: ['facet', 'selected'],

  emits: ['range-selected'],

  data() {
    return {
      minValue: null,

      maxValue: null,

      list: []
    }
  },

  computed: {
    showMinLimit: function() {
      const {op} = this.facet;
      return op == 'GT' || op == 'GE' || op == 'BETWEEN' || op == 'ANY' || op == 'EXISTS';
    },

    showMaxLimit: function() {
      const {op} = this.facet;
      return op == 'LT' || op == 'LE' || op == 'BETWEEN' || op == 'ANY' || op == 'EXISTS';
    }
  },

  watch: {
    selected: function() {
      this.refresh();
    }
  },

  mounted() {
    this.refresh();
  },

  methods: {
    refresh: function() {
      const {values: {minValue, maxValue}} = this.selected || {values: {}};
      if (minValue || minValue === 0 || maxValue || maxValue === 0) {
        this.list = [{minValue, maxValue, selected: true}];
      } else {
        this.list = [];
      }
    },

    addLimit: function() {
      if (this.minValue || this.minValue === 0 || this.maxValue || this.maxValue === 0) {
        const range = {minValue: this.minValue, maxValue: this.maxValue, selected: true};
        this.$emit('range-selected', {facet: this.facet, range});
      }

      this.minValue = this.maxValue = null;
    },

    addDateLimit: function() {
      const {shortDateFmt} = this.$ui.global.locale;
      if (this.minValue || this.maxValue) {
        const range = {
          minValue: this.minValue ? util.formatDate(this.minValue, shortDateFmt) : null,
          maxValue: this.maxValue ? util.formatDate(this.maxValue, shortDateFmt) : null,
          selected: true
        };
        this.$emit('range-selected', {facet: this.facet, range});
      }

      this.minValue = this.maxValue = null;
    },

    toggleSelection: function(item) {
      if (item.selected) {
        this.$emit('range-selected', {facet: this.facet, range: item});
      } else {
        this.$emit('range-selected', {facet: this.facet, range: null});
      }
    }
  }
}
</script>

<style scoped>
.range-facet {
  list-style: none;
  margin: 0;
  padding: 0;
}       
        
.range-facet li {
  display: flex;
  flex-direction: row;
}       
      
.range-facet li.input {
  margin-bottom: 1.25rem;
}
            
.range-facet li.input :deep(.os-input-number),
.range-facet li.input :deep(.os-date-picker) {
  width: 100%;
  margin-right: 1rem;
}
  
.range-facet li :deep(.os-boolean-checkbox) {
  margin-right: 1rem;
}
</style>
