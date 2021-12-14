<template>
  <div class="os-list os-list-hover" :class="{'show-filters': showFilters}">
    <div class="results">
      <div class="info" v-if="loading || list.length == 0">
        <div v-show="loading">
          <os-message type="info"><span>Loading records, please wait for a moment...</span></os-message>
        </div>
        <div v-show="!loading && list.length == 0">
          <os-message type="info"><span>No records to show</span></os-message>
        </div>
      </div>

      <div v-else class="results-inner">
        <div v-if="selectedRows.length > 0" class="p-inline-message p-inline-message-info">
          <span v-show="selectedRows.length == 1">1 record selected</span>
          <span v-show="selectedRows.length > 1">{{selectedRows.length}} records selected</span>
        </div>
        <data-table :value="list" v-model:selection="selectedRows" @row-click="rowClick($event)">
          <column class="os-selection-cb" v-if="allowSelection" selectionMode="multiple"></column>
          <column v-for="column of columns" :header="column.caption" :key="column.name" :style="column.uiStyle">
            <template #body="slotProps">
              <span v-if="column.href">
                <a :href="column.href(slotProps.data)" :target="column.hrefTarget">
                  <span>{{columnValue(slotProps.data, column)}}</span>
                </a>
              </span>
              <span v-else-if="column.type == 'component'">
                <component :is="column.component" v-bind="column.data(slotProps.data.rowObject)" />
              </span>
              <span v-else>
                <span>{{columnValue(slotProps.data, column)}}</span>
              </span>
            </template>
          </column>
          <column v-if="showRowActions">
            <template #body="slotProps">
              <div class="os-click-esc actions">
                <slot name="rowActions" :rowObject="slotProps.data.rowObject"> </slot>
              </div>
            </template>
          </column>
        </data-table>
      </div>

      <slot name="footer"></slot>
    </div>
    <div class="filters">
      <div class="filters-inner">
        <div class="title">
          <span>Filters</span>
        </div>
        <div class="body">
          <form-group dense v-for="filter of filters" :key="filter.name">
            <cell :width="12">
              <span v-if="filter.type == 'text'">
                <input-text md-type="true" :placeholder="filter.caption" v-model="filterValues[filter.name]"/>
              </span>
              <span v-if="filter.type == 'dropdown'">
                <dropdown md-type="true" :placeholder="filter.caption" v-model="filterValues[filter.name]"
                  :list-source="filter.listSource">
                </dropdown>
              </span>
              <span v-if="filter.type == 'site'">
                <os-site-dropdown md-type="true" :placeholder="filter.caption" v-model="filterValues[filter.name]"
                  :list-source="filter.listSource" :context="filterValues">
                </os-site-dropdown>
              </span>
            </cell>
          </form-group>

          <form-group>
            <cell :width="12">
              <Button style="width: 100%" label="Clear Filters" @click="clearFilters"/>
            </cell>
          </form-group>

          <form-group>
            <cell :width="12">
              <os-label class="underlined">Records to Display</os-label>
            </cell>

            <cell :width="12">
              <os-radio-button v-show="pageSizeOpts.pageSize" name="pageSize" class="inline"
                :options="pageSizeOpts.sizes" v-model="pageSizeOpts.pageSize"
                @change="changePageSize" />

              <div class="input-group" v-show="!pageSizeOpts.pageSize">
                <input-text md-type="true" v-model="pageSizeOpts.customPageSize" placeholder="Custom value" />
                <Button label="Go" @click="updatePageSize"/>
                <Button left-icon="times" @click="clearPageSize"/>
              </div>
            </cell>
          </form-group>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import DataTable from 'primevue/datatable';
import Column from 'primevue/column';

import FormGroup from '@/common/components/FormGroup.vue';
import Col from '@/common/components/Col.vue';
import InputText from '@/common/components/InputText.vue';
import Label from '@/common/components/Label.vue';
import Dropdown from '@/common/components/Dropdown.vue';
import Button from '@/common/components/Button.vue';
import RadioButton from '@/common/components/RadioButton.vue';
import Message from '@/common/components/Message.vue';

import exprUtil from '@/common/services/ExpressionUtil.js';
import util     from '@/common/services/Util.js';

export default {
  props: [
    'data',
    'columns',
    'filters',
    'query',
    'allowSelection',
    'loading',
    'showRowActions'
  ],

  emits: ['selectedRows', 'filtersUpdated', 'pageSizeChanged', 'rowClicked'],

  components: {
    'data-table': DataTable,
    'column': Column,
    'form-group': FormGroup,
    'cell': Col,
    'input-text': InputText,
    'dropdown': Dropdown,
    'os-label': Label,
    'os-radio-button': RadioButton,
    'os-message': Message,
    Button
  },

  setup() {
    return {
      debounce: (function() {
        let timeout = null;
        return function(fn, delayMs) {
          clearTimeout(timeout);
          timeout = setTimeout(() => { fn(); }, delayMs || 500);
        };
      })()
    }
  },

  data() {
    return {
      showFilters: false,

      filterValues: { },

      selectedRows: [],

      pageSizeOpts: {
        currentPageSize: 100,

        pageSize: 100,

        sizes: [
          {caption: '100', value: 100},
          {caption: '200', value: 200},
          {caption: '500', value: 500},
          {caption: 'Custom', value: ''},
        ]
      }
    }
  },

  mounted() {
    let values = {};
    if (this.query) {
      values = JSON.parse(decodeURIComponent(atob(this.query)));
    }

    Object.assign(this.filterValues, values);
    if (Object.keys(values).length > 0) {
      this.showFilters = true;
    } else {
      this.emitFiltersUpdated();
    }
  },

  methods: {
    toggleShowFilters: function() {
      this.showFilters = !this.showFilters;
    },

    clearFilters: function() {
      this.filters.forEach((filter) => delete this.filterValues[filter.name]);
    },

    emitFiltersUpdated: function() {
      let fb = util.uriEncode(this.filterValues);
      let event = {filters: this.filterValues, uriEncoding: fb, pageSize: this.pageSizeOpts.currentPageSize + 1};
      this.$emit('filtersUpdated', event);
    },

    changePageSize: function(input) {
      let pageSize = +input;
      if (isNaN(pageSize) || pageSize <= 0 || this.pageSizeOpts.currentPageSize == pageSize) {
        return;
      }

      let oldPageSize = this.pageSizeOpts.currentPageSize;
      this.pageSizeOpts.currentPageSize = pageSize;
      this.$emit('pageSizeChanged', pageSize);
      if (this.data.length < oldPageSize && this.data.length < pageSize) {
        return;
      }

      this.emitFiltersUpdated();
    },

    updatePageSize: function() {
      if (this.pageSizeOpts.customPageSize && !isNaN(+this.pageSizeOpts.customPageSize)) {
        this.changePageSize(this.pageSizeOpts.customPageSize);
      }
    },

    clearPageSize: function() {
      this.pageSizeOpts.pageSize = this.pageSizeOpts.currentPageSize;
    },

    reload: function() {
      this.selectedRows = [];
      this.emitFiltersUpdated();
    },

    rowClick: function(row) {
      let el = row.originalEvent.target;
      while (el != null && el.tagName.toUpperCase() != 'TR') {
        if (el.className &&
          typeof el.className.indexOf == 'function' &&
          (el.className.indexOf('os-selection-cb') != -1 || el.className.indexOf('os-click-esc') != -1)) {
          return;
        }

        el = el.parentNode;
      }

      this.$emit('rowClicked', row.data.rowObject);
    },

    columnValue: function(data, column) {
      let value = undefined;
      if (typeof column.value == 'function') {
        value = column.value(data.rowObject);
      } else {
        value = exprUtil.eval(data.rowObject, column.name);
        if (value) {
          let extra = {metadata: column, context: data.rowObject};
          if (column.type == 'user') {
            value = this.$filters.username(value, extra);
          } else if (column.type == 'date') {
            value = this.$filters.date(value, extra);
          } else if (column.type == 'date-time' || column.type == 'dateTime') {
            value = this.$filters.dateTime(value, extra);
          } else if (column.type == 'storage-position') {
            value = this.$filters.storagePosition(value, extra);
          } else if (column.type == 'specimen-measure') {
            value = this.$filters.specimenMeasure(value, extra);
          }
        }
      }

      return value || '-';
    }
  },

  computed: {
    list() {
      let input      = this.data || [];
      let columnDefs = this.columns || [];

      let result = [];
      let length = input.length > this.pageSizeOpts.currentPageSize ? input.length - 1 : input.length;
      for (let rowIdx = 0; rowIdx < length; ++rowIdx) {
        let row = {rowObject: input[rowIdx]};

        for (let colIdx = 0; colIdx < columnDefs.length; ++colIdx) {
          let cd = columnDefs[colIdx];
          if (cd.value) {
            row[cd.name] = cd.value(input[rowIdx]);
          } else {
            row[cd.name] = input[rowIdx][cd.name];
          }
        }

        result.push(row);
      }

      return result;
    }
  },

  watch: {
    filterValues: {
      deep: true,

      handler() {
        this.debounce(() => this.emitFiltersUpdated());
      }
    },

    selectedRows: {
      handler(newVal) {
        this.$emit('selectedRows', newVal);
      }
    }
  }
}
</script>

<style scoped>
  
.os-list {
  overflow: auto;
  height: 100%;
  display: flex;
  flex-direction: row;
}

.os-list:after {
  content: ' ';
  clear: both;
  display: block;
}

.os-list .results {
  display: flex;
  height: 100%;
  flex-direction: column;
  width: 100%;
}

.os-list.show-filters .results {
  width: 75%;
}

.os-list .results .info {
  padding-right: 15px;
}

.os-list .results .results-inner {
  overflow: auto;
  padding-right: 15px;
  height: 100%;
}

.os-list .results .results-inner .p-inline-message {
  width: 100%;
}

.os-list .results .results-inner .p-inline-message span {
  width: 100%;
  text-align: left;
}

.os-list .filters {
  width: 25%;
  display: none;
  flex-direction: column;
}

.os-list.show-filters .filters {
  display: flex;
  border-left: 1px solid #ddd;
}

.os-list .filters .filters-inner {
  overflow: auto;
  padding: 0px 15px 15px;
  height: 100%;
}

.filters .title {
  margin: 0px;
  font-size: 14px;
  font-weight: bold;
  color: #333!important;
  line-height: 1.42;
  padding: 8px 0px;
  border-bottom: 2px solid #ddd;
}

.filters .body {
  margin-top: 25px;
}

.filters .body .form-group {
  margin-bottom: 30px;
}

.os-list :deep(table) {
  width: 100%;
  margin-bottom: 20px;
  display: table;
  border-collapse: collapse;
  table-layout: inherit;
}

.os-list :deep(tr) {
  margin-right: 0px;
  margin-left: 0px;
}

.os-list :deep(thead tr th),
.os-list :deep(tbody tr td) {
  padding: 8px;
  line-height: 1.42857143;
  vertical-align: top;
  border-top: 1px solid #ddd;
  word-break: break-word;
}        
        
.os-list :deep(thead tr th) {
  vertical-align: bottom;
  border-bottom: 1px solid #ddd;
  font-weight: bold;
}

.os-list :deep(thead tr th) {
  white-space: nowrap;
}
    
.os-list :deep(thead tr:first-child th) {
  border-top: 0;
}   

.os-list-hover :deep(tbody tr:hover) {
  background: #f7f7f7;
  cursor: pointer;
}

.os-list :deep(.p-datatable .p-datatable-tbody > tr.p-highlight) {
  /*background: #E3F2FD;
  color: #495057;*/
  background: inherit;
  color: inherit;
}

.os-list :deep(.p-checkbox .p-checkbox-box.p-highlight) {
  background: #2196F3;
  border-color: #2196F3;
}

.os-list :deep(th.os-selection-cb) {
  vertical-align: middle;
}

.os-list :deep(.os-selection-cb .p-checkbox) {
  height: 15px;
  width: 15px;
  vertical-align: text-top;
}

.os-list :deep(.os-selection-cb .p-checkbox .p-checkbox-box) {
  height: 15px;
  width: 15px;
}

.filters .body .input-group {
  display: flex;
  align-items: flex-end;
}

.filters .body .input-group :deep(.os-input-text) {
  width: 100%;
}
</style>
