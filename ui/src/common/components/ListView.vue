<template>
  <div class="os-list os-list-hover" :class="{'show-filters': showFilters}" v-if="!summaryView">
    <div class="results">
      <div class="info" v-if="loading || list.length == 0">
        <div v-show="loading">
          <os-message type="info">
            <span v-t="'common.lists.loading'">Loading records, please wait for a moment...</span>
          </os-message>
        </div>
        <div v-show="!loading && list.length == 0">
          <os-message type="info">
            <span v-t="'common.lists.no_records'">No records to show</span>
          </os-message>
        </div>
      </div>

      <div v-else class="results-inner">
        <div v-if="selectedRows.length > 0" class="p-inline-message p-inline-message-info">
          <span v-t="{path: 'common.lists.records_selected', args: {count: selectedRows.length}}"></span>
        </div>
        <data-table :value="list" v-model:selection="selectedRows" @row-click="rowClick($event)" @sort="sort($event)">
          <column class="os-selection-cb" v-if="allowSelection" selectionMode="multiple"></column>
          <column v-for="column of schema.columns" :header="caption(column)" :key="column.name" :field="column.name"
            :style="column.uiStyle" :sortable="column.sortable">
            <template #body="slotProps">
              <span v-if="column.href">
                <a :href="column.href(slotProps.data, $route.query)" :target="column.hrefTarget">
                  <span>{{columnValue(slotProps.data, column)}}</span>
                </a>
              </span>
              <component v-else-if="column.type == 'component'" :is="column.component"
                v-bind="column.data(slotProps.data.rowObject)" @star-toggled="rowStarToggled(slotProps.data)" />
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
          <template #footer v-if="$slots.footerRow">
            <slot name="footerRow"> </slot>
          </template>
        </data-table>
      </div>

      <slot name="footer"></slot>
    </div>

    <div class="filters">
      <div class="filters-inner">
        <div class="title">
          <span v-t="'common.lists.filters'">Filters</span>
        </div>
        <div class="body">
          <form-group dense v-for="filter of searchFilters" :key="filter.name">
            <cell :width="12">
              <span v-if="filter.type == 'text'">
                <os-input-text md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]" />
              </span>
              <span v-else-if="filter.type == 'number'">
                <div class="range" v-if="filter.range">
                  <os-input-number md-type="true" :placeholder="'Min. ' + caption(filter)"
                    v-model="filterValues[filter.name + '.$min']" :max-fraction-digits="filter.maxFractionDigits" />
                  <os-input-number md-type="true" :placeholder="'Max. ' + caption(filter)"
                    v-model="filterValues[filter.name + '.$max']" :max-fraction-digits="filter.maxFractionDigits" />
                </div>
                <div v-else>
                  <os-input-number md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                    :max-fraction-digits="filter.maxFractionDigits" />
                </div>
              </span>
              <span v-else-if="filter.type == 'dropdown'">
                <dropdown md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                  :list-source="filter.listSource">
                </dropdown>
              </span>
              <span v-else-if="filter.type == 'pv'">
                <os-pv-dropdown md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                  :attribute="filter.attribute" :leafValue="filter.leafValue" :selectProp="filter.selectProp">
                </os-pv-dropdown>
              </span>
              <span v-else-if="filter.type == 'site'">
                <os-site-dropdown md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                  :list-source="filter.listSource" :context="filtersContext">
                </os-site-dropdown>
              </span>
              <span v-else-if="filter.type == 'user'">
                <os-user-dropdown md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                  :select-prop="filter.selectProp" :context="filtersContext">
                </os-user-dropdown>
              </span>
              <span v-else-if="filter.type == 'date'">
                <div class="range" v-if="filter.range">
                  <os-date-picker md-type="true" :placeholder="'Min. ' + caption(filter)"
                    v-model="filterValues[filter.name + '.$min']"
                    @update:model-value="handleDateInput(filter, filter.name + '.$min')">
                  </os-date-picker>
                  <os-date-picker md-type="true" :placeholder="'Max. ' + caption(filter)"
                    v-model="filterValues[filter.name + '.$max']"
                    @update:model-value="handleDateInput(filter, filter.name + '.$max')">
                  </os-date-picker>
                </div>
                <div v-else>
                  <os-date-picker md-type="true" :placeholder="caption(filter)"
                    v-model="filterValues[filter.name]" @update:model-value="handleDateInput(filter)">
                  </os-date-picker>
                </div>
              </span>
            </cell>
          </form-group>

          <form-group>
            <cell :width="12">
              <Button style="width: 100%" :label="$t('common.lists.clear_filters')" @click="clearFilters"/>
            </cell>
          </form-group>

          <form-group>
            <cell :width="12">
              <os-label class="underlined">
                <span v-t="'common.lists.records_to_display'">Records to Display</span>
              </os-label>
            </cell>

            <cell :width="12">
              <os-radio-button v-show="pageSizeOpts.pageSize" name="pageSize" class="inline"
                :options="pageSizeOpts.sizes" :optionsPerRow="4" v-model="pageSizeOpts.pageSize"
                @change="changePageSize" />

              <div class="input-group" v-show="!pageSizeOpts.pageSize">
                <input-text md-type="true" v-model="pageSizeOpts.customPageSize"
                  :placeholder="$t('common.lists.custom_value')" />

                <Button :label="$t('common.lists.go')" @click="updatePageSize"/>
                <Button left-icon="times" @click="clearPageSize"/>
              </div>
            </cell>
          </form-group>
        </div>
      </div>
    </div>
  </div>

  <div class="os-list-items" v-else-if="schema.summary">
    <div class="item" :ref="'item' + idx" v-for="(item, idx) of list" :key="idx" @click="itemSelected($event, item)"
      :class="{active: selectedItem && item.rowObject == selectedItem}">
      <div class="header">
        <os-icon :name="item.$summary.icon" v-if="item.$summary.icon" />
        <a class="os-click-esc" :href="item.$summary.url" v-if="item.$summary.url">
          <span>{{item.$summary.title}}</span>
        </a>
      </div>
      <div class="descriptions">
        <div v-for="(description, descIdx) of item.$summary.descriptions" :key="descIdx">
          <span>{{description}}</span>
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
    'context',
    'data',
    'schema',
    'query',
    'selected',
    'allowSelection',
    'loading',
    'showRowActions'
  ],

  emits: ['selectedRows', 'filtersUpdated', 'pageSizeChanged', 'rowClicked', 'sort', 'rowStarToggled'],

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
      summaryView: false,

      showFilters: false,

      filterValues: { },

      filtersContext: { },

      selectedRows: [],

      selectedItem: null,

      pageSizeOpts: {
        currentPageSize: 100,

        pageSize: 100,

        sizes: [
          {caption: '100', value: 100},
          {caption: '200', value: 200},
          {caption: '500', value: 500},
          {caption: this.$t('common.lists.custom_value'), value: ''},
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
    Object.assign(this.filtersContext, this.filterValues);

    if (Object.keys(values).length > 0) {
      this.showFilters = true;
    } else {
      this.emitFiltersUpdated();
    }
  },

  methods: {
    switchToSummaryView: function() {
      this.summaryView = true;
    },

    switchToTableView: function() {
      this.summaryView = false;
    },

    toggleShowFilters: function() {
      this.showFilters = !this.showFilters;
    },

    displayFilters: function() {
      this.showFilters = true;
    },

    hideFilters: function() {
      this.showFilters = false;
    },

    clearFilters: function() {
      if (this.searchFilters) {
        this.searchFilters.forEach(
          filter => {
            if (filter.range) {
              delete this.filterValues[filter.name + '.$min'];
              delete this.filterValues[filter.name + '.$max'];
            } else {
              delete this.filterValues[filter.name];
            }
          }
        );
      }
    },

    emitFiltersUpdated: function() {
      const fb = util.uriEncode(this.filterValues);
      const event = {filters: this.filterValues, uriEncoding: fb, pageSize: this.pageSizeOpts.currentPageSize + 1};
      this.$emit('filtersUpdated', event);
    },

    handleDateInput: function(filter, name) {
      const key  = name || filter.name;
      const date = this.filterValues[key];
      if (date && filter.format) {
        this.filterValues[key] = util.formatDate(date, filter.format);
        if (filter.stringLiteral) {
          this.filterValues[key] = '"' + this.filterValues[key] + '"';
        }
      }
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

    caption: function({caption, captionCode, label, labelCode}) {
      if (captionCode) {
        return this.$t(captionCode);
      } else if (labelCode) {
        return this.$t(labelCode);
      } else if (caption) {
        return caption;
      } else if (label) {
        return label;
      }

      return '';
    },

    columnValue: function(data, column) {
      let value = undefined;
      if (typeof column.value == 'function') {
        value = column.value(data.rowObject);
      } else {
        value = exprUtil.eval(data.rowObject, column.name);
        if (value || value == 0) {
          let extra = {metadata: column, context: data.rowObject};
          if (column.type == 'user') {
            value = this.$filters.username(value, extra);
          } else if (column.type == 'date' || (column.type == 'datePicker' && column.showTime != true)) {
            value = this.$filters.date(value, extra);
          } else if (column.type == 'date-time' || column.type == 'dateTime' ||
                      (column.type == 'datePicker' && column.showTime == true)) {
            value = this.$filters.dateTime(value, extra);
          } else if (column.type == 'storage-position') {
            value = this.$filters.storagePosition(value, extra);
          } else if (column.type == 'specimen-measure') {
            value = this.$filters.specimenMeasure(value, extra);
          } else if (value instanceof Array) {
            value = value.join(', ');
          }
        }
      }

      return value || (value === 0 ? value : '-');
    },

    itemSelected: function(event, item) {
      let el = event.target;
      while (el != null) {
        if (el.className && typeof el.className.indexOf == 'function') {
          if (el.className.indexOf('os-selection-cb') != -1 || el.className.indexOf('os-click-esc') != -1) {
            return;
          } else if (el.className.indexOf('item') != -1) {
            break;
          }
        }

        el = el.parentNode;
      }

      this.selectedItem = item.rowObject;
      this.$emit('rowClicked', item.rowObject);
    },

    valueFn: function(input) {
      if (typeof input == 'function') {
        return input;
      }

      return (ro) => input && exprUtil.getValue(ro, input) || '';
    },

    sort: function(event) {
      this.$emit('sort', event);
    },

    rowStarToggled: function({rowObject}) {
      this.$emit('rowStarToggled', rowObject);
    }
  },

  computed: {
    list() {
      const summaryFns = {
        icon: () => '',
        titleText: () => '',
        url: () => '',
        descriptions: []
      };

      if (this.schema.summary) {
        const title = this.schema.summary.title || {};
        summaryFns.icon  = this.valueFn(title.icon);
        summaryFns.titleText = this.valueFn(title.text);
        summaryFns.url = this.valueFn(title.url);
        summaryFns.descriptions = (this.schema.summary.descriptions || []).map(desc => this.valueFn(desc));
      }

      let input      = this.data || [];
      let columnDefs = this.schema.columns || [];

      let result = [];
      let length = input.length > this.pageSizeOpts.currentPageSize ? input.length - 1 : input.length;
      for (let rowIdx = 0; rowIdx < length; ++rowIdx) {
        let row = {idx: rowIdx, rowObject: input[rowIdx]};
        if (this.schema.key) {
          row.key = exprUtil.getValue(input[rowIdx], this.schema.key);
        }

        for (let colIdx = 0; colIdx < columnDefs.length; ++colIdx) {
          let cd = columnDefs[colIdx];
          if (cd.value) {
            row[cd.name] = cd.value(input[rowIdx]);
          } else {
            row[cd.name] = input[rowIdx][cd.name];
          }
        }

        if (this.schema.summary) {
          row.$summary = {
            icon:         summaryFns.icon(row.rowObject),
            title:        summaryFns.titleText(row.rowObject),
            url:          summaryFns.url(row.rowObject, this.$route.query),
            descriptions: summaryFns.descriptions.map(desc => desc(row.rowObject))
          };
        }

        result.push(row);
      }

      return result;
    },

    searchFilters() {
      const filters = this.schema.filters || [];
      return filters.filter(
        (filter) => {
          if (filter.showWhen && !exprUtil.eval(this.context, filter.showWhen)) {
            return false;
          }

          return true;
        }
      );
    }
  },

  watch: {
    filterValues: {
      deep: true,

      handler() {
        this.filtersContext = Object.assign(this.filtersContext, this.filterValues || {});
        this.debounce(() => this.emitFiltersUpdated());
      }
    },

    selectedRows: {
      handler(newVal) {
        this.$emit('selectedRows', newVal);
      }
    },

    selected: {
      handler(newVal) {
        if (newVal && this.selectedItem != newVal) {
          this.selectedItem = newVal;
          const idx = this.list.findIndex(item => item.rowObject == this.selectedItem);
          if (idx >= 0) {
            setTimeout(() => {
              let el = this.$refs['item' + idx];
              if (el instanceof Array && el.length > 0) {
                el = el[0];
              }

              if (el) {
                el.scrollIntoView({behaviour: 'smooth'});
              }
            }, 500);
          }
        }
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
  width: calc(100% - 300px);
}

.os-list .results .info {
  padding-right: 15px;
}

.os-list .results .results-inner {
  overflow: auto;
  padding-right: 15px;
  height: 100%;
}

.os-list .results :deep(.os-pager) {
  margin-top: 1.25rem;
}

.os-list .results .results-inner .p-inline-message {
  width: 100%;
}

.os-list .results .results-inner .p-inline-message span {
  width: 100%;
  text-align: left;
}

.os-list .filters {
  width: 300px;
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

.os-list :deep(.actions) {
  text-align: right;
}

.os-list :deep(.actions .os-icon-wrapper) {
  font-size: 80%!important;
}

.os-list :deep(.actions .btn) {
  padding: 0px 6px;
}

.os-list.os-muted-list-header :deep(table.p-datatable-table > thead.p-datatable-thead > tr > th) {
  background: #f5f5f5;
  color: #707070;
  font-size: 0.75rem;
}

.os-list.os-bordered-list :deep(table) {
  border: 1px solid #ddd;
}

.filters .body .input-group {
  display: flex;
  align-items: flex-end;
}

.filters .body .input-group :deep(.os-input-text) {
  width: 100%;
}

.filters .body .range {
  display: flex;
}

.filters .body .range > div {
  flex: 1;
}

.filters .body .range > div:first-child {
  margin-right: 0.25rem;
}

.filters .body .range > div:last-child {
  margin-left: 0.25rem;
}

.os-list-items {
  margin: -8px -15px;
}

.os-list-items .item {
  padding: 1rem;
  border-bottom: 1px solid #ddd;
}

.os-list-items .item:hover {
  background: #f7f7f7;
  cursor: pointer;
}

.os-list-items .item.active {
  background: #e9ecef;
}

.os-list-items .item .header {
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.os-list-items .item .header .os-icon-wrapper {
  margin-right: 0.5rem;
}

.os-list-items .item .descriptions > div:not(:last-child) {
  margin-bottom: 0.5rem;
}
</style>
