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
            <span v-t="noRecordsMsg || 'common.lists.no_records'">No records to show</span>
          </os-message>
        </div>
      </div>

      <div v-else class="results-inner">
        <div v-if="selectedRows.length > 0" class="p-inline-message p-inline-message-info">
          <span v-t="{path: 'common.lists.records_selected', args: {count: selectedRows.length}}"></span>
        </div>
        <data-table scrollable scroll-height="flex" :value="list"
          v-model:expandedRows="expandedRows" v-model:selection="selectedRows"
          :row-class="getRowClass" @row-click="rowClick($event)" @sort="sort($event)">
          <column class="os-selection-cb" v-if="allowSelection" selectionMode="multiple"></column>
          <column v-for="column of schema.columns" :header="caption(column)" :key="column.name" :field="column.name"
            :style="column.uiStyle" :sortable="column.sortable">
            <template #body="slotProps">
              <span v-if="column.href && column.href(slotProps.data, $route.query)">
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
          <column class="row-actions" v-if="showRowActions">
            <template #body="slotProps">
              <div class="os-click-esc actions">
                <slot name="rowActions" :rowObject="slotProps.data.rowObject"> </slot>
              </div>
            </template>
          </column>
          <template #footer v-if="$slots.footerRow">
            <slot name="footerRow"> </slot>
          </template>
          <template #expansion="slotProps" v-if="$slots.expansionRow">
            <slot name="expansionRow" :rowObject="slotProps.data.rowObject"> </slot>
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
                  :list-source="filter.listSource" :context="filtersContext" v-if="!filter.multiple">
                </dropdown>
                <multiselect md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                  :list-source="filter.listSource" :context="filtersContext" v-else>
                </multiselect>
              </span>
              <span v-else-if="filter.type == 'booleanCheckbox'">
                <os-boolean-checkbox :inline-label="caption(filter)" v-model="filterValues[filter.name]"
                  v-bind="filter" />
              </span>
              <span v-else-if="filter.type == 'pv'">
                <os-pv-dropdown md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                  v-bind="filter" :context="filtersContext" />
              </span>
              <span v-else-if="filter.type == 'site'">
                <os-site-dropdown md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                  :list-source="filter.listSource" :context="filtersContext">
                </os-site-dropdown>
              </span>
              <span v-else-if="filter.type == 'user'">
                <os-user-dropdown md-type="true" :placeholder="caption(filter)" v-model="filterValues[filter.name]"
                  :list-source="filter.listSource" :select-prop="filter.selectProp" :context="filtersContext">
                </os-user-dropdown>
              </span>
              <span v-else-if="filter.type == 'date'">
                <div class="range" v-if="filter.range">
                  <os-date-picker md-type="true" :placeholder="'Min. ' + caption(filter)"
                    v-model="filterValues[filter.name + '.$min']" :show-time="filter.showTime"
                    :date-only="filter.dateOnly" :iso-string="filter.isoString" />
                  <os-date-picker md-type="true" :placeholder="'Max. ' + caption(filter)"
                    v-model="filterValues[filter.name + '.$max']" :show-time="filter.showTime"
                    :date-only="filter.dateOnly" :iso-string="filter.isoString" />
                </div>
                <div v-else>
                  <os-date-picker md-type="true" :placeholder="caption(filter)"
                    v-model="filterValues[filter.name]" :show-time="filter.showTime"
                    :date-only="filter.dateOnly" :iso-string="filter.isoString" />
                </div>
              </span>
              <span v-else-if="filter.type == 'radio'">
                <os-label v-if="caption(filter)">
                  <span>{{caption(filter)}}</span>
                </os-label>
                <os-radio-button :inline-label="caption(filter)" v-model="filterValues[filter.name]"
                  v-bind="filter" />
              </span>
            </cell>
          </form-group>

          <form-group>
            <cell :width="12">
              <Button style="width: 100%" :label="$t('common.lists.clear_filters')" @click="clearFilters"/>
            </cell>
          </form-group>

          <form-group v-if="!hidePageSizeSelector">
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
    <div v-show="!loading && list.length == 0">
      <os-message type="info" style="margin: 0.25rem;">
        <span v-t="noRecordsMsg || 'common.lists.no_records'">No records to show</span>
      </os-message>
    </div>

    <div class="item" :ref="'item' + idx" v-for="(item, idx) of list" :key="idx" @click="itemSelected($event, item)"
      :class="{active: selectedItem && (item.rowObject == selectedItem || (item.key && item.key == selectedItem.key))}">
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
      <div class="status" v-if="item.$summary.status && item.$summary.status.component">
        <component :is="item.$summary.status.component" v-bind="item.$summary.status.data" />
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
import MultiSelectDropdown from '@/common/components/MultiSelectDropdown.vue';
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
    'expanded',
    'allowSelection',
    'loading',
    'showRowActions',
    'noRecordsMsg',
    'rowClass',
    'idFilter',
    'hidePageSizeSelector'
  ],

  emits: [
    'selectedRows',
    'filtersUpdated',
    'pageSizeChanged',
    'rowClicked',
    'sort',
    'rowStarToggled'
  ],

  components: {
    'data-table': DataTable,
    'column': Column,
    'form-group': FormGroup,
    'cell': Col,
    'input-text': InputText,
    'dropdown': Dropdown,
    'multiselect': MultiSelectDropdown,
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

      filtersContext: { _formCache: {} },

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
      Object.keys(this.filterValues).forEach(key => delete this.filterValues[key]);
    },

    emitFiltersUpdated: function() {
      //
      // first remove the filters that are hidden or not displayed
      //
      const searchFilters = this.searchFilters;
      for (let filterKey of Object.keys(this.filterValues || {})) {
        let found = false;
        for (let {name} of searchFilters) {
          if (name == filterKey || (name + '.$min') == filterKey || (name + '.$max') == filterKey) {
            found = true;
            break;
          }
        }

        if (!found && filterKey != this.idFilter) {
          delete this.filterValues[filterKey];
        }
      }

      const fb = util.uriEncode(this.filterValues);
      const event = {filters: this.filterValues, uriEncoding: fb, pageSize: this.pageSizeOpts.currentPageSize + 1};
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

    clearSelection: function() {
      this.selectedRows = [];
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

    getRowClass: function(row) {
      const {rowObject} = row;
      if (typeof this.schema.rowClass == 'function') {
        return this.schema.rowClass(rowObject);
      } else if (typeof this.rowClass == 'function') {
        return this.rowClass(rowObject);
      }

      return undefined;
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
            if (value instanceof Array) {
              value = value.map(u => this.$filters.username(u, extra)).join(', ');
            } else {
              value = this.$filters.username(value, extra);
            }
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
          } else if (column.type == 'booleanCheckbox') {
            value = this.$filters.boolValue(value);
          }
        }
      }

      if ((value == undefined || value == null) && typeof column.empty == 'function') {
        return column.empty(data.rowObject);
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
      const {sortField} = event;
      const sortOrders = this.sortOrders = this.sortOrders || {};

      let sortOrder = null;
      if (sortOrders[sortField] == 1) {
        sortOrder = sortOrders[sortField] = -1;
      } else {
        sortOrder = sortOrders[sortField] = 1;
      }

      this.$emit('sort', {...event, sortOrder});
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
        descriptions: [],
        status: () => null
      };

      if (this.schema.summary) {
        const title = this.schema.summary.title || {};
        summaryFns.icon  = this.valueFn(title.icon);
        summaryFns.titleText = this.valueFn(title.text);
        summaryFns.url = this.valueFn(title.url);
        summaryFns.descriptions = (this.schema.summary.descriptions || []).map(desc => this.valueFn(desc));
        summaryFns.status = typeof this.schema.summary.status == 'function' ? this.schema.summary.status : () => null
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
            descriptions: summaryFns.descriptions.map(desc => desc(row.rowObject)),
            status      : summaryFns.status(row.rowObject)
          };
        }

        result.push(row);
      }

      return result;
    },

    expandedRows: function() {
      const result = [];
      if (!this.expanded || this.expanded.length == 0) {
        return [];
      }

      for (let row of this.list) {
        if (this.expanded.indexOf(row.rowObject) != -1) {
          result.push(row);
        }
      }

      return result;
    },

    searchFilters() {
      const filters      = this.schema.filters || [];
      const filterValues = this.filterValues || {};
      const context      = {...this.context, filterValues};
      return filters.filter(
        (filter) => {
          if (filter.showWhen && !exprUtil.eval(context, filter.showWhen)) {
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
          let idx = this.list.findIndex(item => item.rowObject == this.selectedItem);
          if (idx == -1 && this.schema.key) {
            const selectedKey = this.selectedItem.key = exprUtil.getValue(this.selectedItem, this.schema.key);
            idx = this.list.findIndex(item => item.key && item.key == selectedKey);
          }

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

.os-list :deep(table.p-datatable-table) {
  width: 100%;
  margin-bottom: 20px;
  display: table;
  border-collapse: collapse;
  table-layout: inherit;
}

.os-list :deep(table.p-datatable-table > thead > tr),
.os-list :deep(table.p-datatable-table > tbody > tr) {
  margin-right: 0px;
  margin-left: 0px;
}

.os-list :deep(table.p-datatable-table > thead > tr > th),
.os-list :deep(table.p-datatable-table > tbody > tr > td) {
  padding: 8px;
  line-height: 1.42857143;
  vertical-align: top;
  border-top: 1px solid #ddd;
  word-break: break-word;
}        
        
.os-list :deep(table.p-datatable-table > thead > tr > th) {
  vertical-align: bottom;
  border-bottom: 1px solid #ddd;
  font-weight: bold;
}

.os-list :deep(table.p-datatable-table > thead > tr > th) {
  white-space: nowrap;
}
    
.os-list :deep(table.p-datatable-table > thead > tr:first-child > th) {
  border-top: 0;
}   

.os-list-hover :deep(tbody.p-datatable-tbody > tr:hover) {
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
  /*box-shadow: rgba(50, 50, 93, 0.25) 0px 13px 27px -5px, rgba(0, 0, 0, 0.3) 0px 8px 16px -8px;*/
  /*box-shadow: rgba(50, 50, 93, 0.25) 0px 6px 12px -2px, rgba(0, 0, 0, 0.3) 0px 3px 7px -3px;*/
  box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .3),0 2px 6px 2px rgba(60, 64, 67, .15);
  margin: 1rem;
  border-radius: 0.5rem;
}

.os-list-items .item:hover {
  background: #f7f7f7;
  cursor: pointer;
}

.os-list-items .item.active {
  /*background: #e9ecef;*/
  border: 0.125rem solid #428bca;
}

.os-list-items .item .header {
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.os-list-items .item .header .os-icon-wrapper {
  margin-right: 0.5rem;
}

.os-list-items .item .descriptions {
  white-space: pre-wrap;
}

.os-list-items .item .descriptions > div:not(:last-child) {
  margin-bottom: 0.5rem;
}

.os-list-items .item .status {
  margin: 0.5rem 0rem;
}

.os-list-shadowed-rows.os-list .results .results-inner {
  padding-right: 0rem;
}

.os-list-shadowed-rows :deep(table.p-datatable-table) {
  border: 0px;
  border-collapse: separate;
  border-spacing: 2px 15px;
  margin-top: -0.5rem;
  margin-bottom: 0rem;
  table-layout: fixed;
}

.os-list-shadowed-rows :deep(.os-key-values .item) {
  border-spacing: 0px;
}

.os-list-shadowed-rows :deep(table.p-datatable-table > thead > tr > th) {
  background: transparent!important;
  border-bottom: 0;
  padding: 0rem 1rem;
  text-align: center;
}

.os-list-shadowed-rows :deep(table.p-datatable-table > thead > tr > th.row-actions) {
  width: 90px;
}

.os-list-shadowed-rows :deep(table.p-datatable-table > tbody > tr > td) {
  border-top: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.os-list-shadowed-rows :deep(table.p-datatable-table > tbody > tr) {
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 2px 6px 2px rgba(60,64,67,.15);
  border-radius: 0.5rem;
  background: transparent;
}

.os-list-shadowed-rows :deep(table.p-datatable-table > tbody > tr:hover) {
  background: transparent;
}

.os-list-shadowed-rows :deep(table.p-datatable-table > tbody > tr > td) {
  vertical-align: middle;
  padding: 1rem;
}
</style>
