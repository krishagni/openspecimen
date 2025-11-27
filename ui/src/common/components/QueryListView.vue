<template>
  <os-list-view
    :schema="schema"
    :data="data"
    :allow-selection="allowSelection"
    :selected="selected"
    :query="query"
    :id-filter="idFilter"
    :loading="loading"
    :show-row-actions="showRowActions"
    @filters-updated="loadList"
    @selected-rows="onRowsSelection"
    @row-clicked="rowClicked"
    @row-star-toggled="rowStarToggled"
    @sort="sort"
    ref="listView" v-if="showList">

    <template #rowActions="{rowObject}" v-if="showRowActions">
      <slot name="actions" :row-object="rowObject" />
    </template>
  </os-list-view>
</template>

<script>

import ui       from '@/global.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';

export default {
  props: [
    'name',
    'objectId',
    'url',
    'newTab',
    'allowSelection',
    'selected',
    'query',
    'idFilter',
    'autoSearchOpen',
    'allowStarring',
    'showRowActions',
    'otherParams'
  ],

  emits: ['selectedRows', 'rowClicked', 'listLoaded', 'rowStarToggled'],

  data() {
    return {
      config: {},

      list: {},

      pageSize: undefined,

      size: undefined, // size of actual list

      schema: {},

      data: [],

      selectedSpecimens: [],

      loading: true,

      showList: false
    };
  },

  async created() {
    const listId = this.listId = {listName: this.name, objectId: this.objectId};
    this.config = await http.get('lists/config', listId);

    const cache = {};
    this.schema.filters = this.filters = (this.config.filters || []).map(
      (filter) => {
        const result = { name: filter.expr, caption: filter.caption };
        if (filter.dataType == 'STRING') {
          if (filter.searchType == 'contains') {
            result.type = 'text';
          } else {
            result.type = 'dropdown';
            result.listSource = {
              loadFn: async function({query}) {
                let result;

                const key = filter.expr + (query ? ':' + query : '');
                if (cache[key]) {
                  result = cache[key];
                } else if (query && cache[filter.expr] && cache[filter.expr].length < 500) {
                  result = cache[key] = cache[filter.expr]
                    .filter(({value}) => value.toLowerCase().indexOf(query.toLowerCase()) != -1);
                } else {
                  const qp     = {...listId, expr: filter.expr, searchTerm: query};
                  const values = await http.get('lists/expression-values', qp);
                  result = cache[key] = values.map(value => ({value}));
                }

                return result;
              },
              selectProp: 'value',
              displayProp: 'value'
            };
          }
        } else if (filter.dataType == 'FLOAT' || filter.dataType == 'INTEGER') {
          result.type = 'number';
          result.range = true;
          result.maxFractionDigits = filter.dataType == 'FLOAT' ? 6 : 0;
        } else if (filter.dataType == 'DATE') {
          result.type = 'date';
          result.range = true;
          result.format = ui.global.locale.shortDateFmt;
          result.stringLiteral = true;
        } else if (filter.dataType == 'BOOLEAN') {
          result.type = 'dropdown';
          result.listSource = {
            options: [
              {value: 1, caption: 'Yes'},
              {value: 0, caption: 'No'}
            ],
            selectProp: 'value',
            displayProp: 'caption'
          };
        }

        return result;
      }
    );

    this.showList = true;
  },

  watch: {
    objectId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.listId.objectId = newVal;
        this.reload();
      }
    }
  },

  methods: {
    searchReq: function(filters) {
      const rangeFilters = {};

      const result = [];
      Object.keys(filters || {}).forEach(
        (filter) => {
          if (!filters[filter] && filters[filter] !== 0) {
            return;
          }

          if (filter.endsWith('.$min') || filter.endsWith('.$max')) {
            const key = filter.substr(0, filter.lastIndexOf('.'));
            rangeFilters[key] = rangeFilters[key] || [];

            let value = filters[filter];
            if (this._isDateFilter(key)) {
              value = this._toDateString(value);
            }

            if (filter.endsWith('.$min')) {
              rangeFilters[key][0] = value;
            } else {
              rangeFilters[key][1] = value;
            }
          } else {
            let values = filters[filter];
            if (!(values instanceof Array)) {
              if (this._isDateFilter(filter)) {
                values = this._toDateString(values);
              }

              values = [values];
            }

            result.push({expr: filter, values: values});
          }
        }
      );

      Object.keys(rangeFilters).forEach(filter => result.push({expr: filter, values: rangeFilters[filter]}));
      return result;
    },

    loadList: async function({filters, pageSize, orderBy, orderDirection}) {
      this.pageSize       = pageSize;
      this.searchFilters  = filters;
      this.search         = this.searchReq(filters);

      this.loading = true;

      const otherParams = this.otherParams || {};
      this.list = await http.post(
        'lists/data',
        this.search,
        {
          ...this.listId,
          includeCount: this.includeCount,
          orderBy: orderBy,
          orderDirection: orderDirection,
          maxResults: pageSize,
          ...otherParams
        }
      );
      this.loading = false;
      
      this.schema = {filters: this.filters};
      const columns = this.schema.columns = this.list.columns.map(
        (column, idx) => this._getResultColumn(column, idx));
      const startIdx = columns.length;
      if (this.list.fixedColumns instanceof Array) {
        Array.prototype.push.apply(
          columns,
          this.list.fixedColumns.map((column, idx) => this._getResultColumn(column, startIdx + idx, false))
        );
      }

      let urlColumnIdx = columns.findIndex(column => column.href);
      if (urlColumnIdx < 0) {
        urlColumnIdx = 0;
      }

      const urlColumn = columns[urlColumnIdx];
      this.schema.summary = {
        title: {
          text: (ro) => exprUtil.eval(ro, urlColumn.name),
          url:  (ro) => '' + exprUtil.eval(ro, this.url)
        },
        descriptions: columns.map(column => column.name).filter((column, index) => index != urlColumnIdx)
      };

      this.data = this.list.rows.map(
        (row) => {
          const columnData = row.data.reduce((acc, item, idx) => { acc['a_' + idx] = item; return acc; }, {});
          if (row.fixedData instanceof Array) {
            const fixedData = row.fixedData.reduce(
              (acc, item, idx) => { acc['a_' + (startIdx + idx)] = item; return acc; }, {}
            );
            Object.assign(columnData, fixedData);
          }

          const hidden = row.hidden || {};
          return {
            column: columnData,
            hidden: hidden,
            ...hidden
          }
        }
      );

      if (this.includeCount) {
        this.size = this.list.size;
      }

      if (this.config.hideEmptyColumns) {
        this.schema.columns = this.schema.columns.filter(
          (col, idx) => {
            const attr = 'a_' + idx;
            return this.data.some(
              (row) => row.column[attr] != 'Not Specified' && (row.column[attr] || row.column[attr] == 0)
            );
          }
        );
      }

      if (this.list.icons) {
        const el = document.createElement('el');
        el.innerHTML = this.list.icons;
        for (let child of el.children) {
          this.schema.columns.unshift({
            type: 'component',
            component: child.tagName.toLowerCase(),
            data: function(rowObject) {
              const {hidden} = rowObject;
              const data = {};
              for (let {nodeName, nodeValue} of child.attributes) {
                data[nodeName] = nodeValue == 'row' ? hidden : exprUtil.getValue(rowObject, nodeValue);
              }

              return data;
            }
          });
        }
      }

      if (this.allowStarring) {
        this.schema.columns.unshift({type: 'component', component: 'os-star', data: ({starred}) => ({starred})});
      }

      const hasFilters = this.hasFilters() && Object.values(filters || {}).length == 0;
      if (this.autoSearchOpen == true && this.data.length > 12 && hasFilters) {
        setTimeout(() => this.$refs.listView.displayFilters(), 0);
      }

      const fb = util.uriEncode(filters || {});
      this.$emit('listLoaded', {widget: this, list: this.list, filters: fb});
    },

    reload: function() {
      this.$refs.listView.reload();
    },

    clearFilters: function() {
      this.$refs.listView.clearFilters();
    },

    loadListSize: async function() {
      if (this.includeCount) {
        return;
      }

      this.includeCount = true;
      const resp = await http.post('lists/size', this.search, this.listId); 
      this.size = resp.size;
    },

    hasFilters: function() {
      return !!this.filters && this.filters.length > 0;
    },

    toggleShowFilters: function() {
      this.$refs.listView.toggleShowFilters();
    },

    onRowsSelection: function(selection) {
      this.$emit('selectedRows', selection);
    },

    rowClicked: function(event) {
      this.$emit('rowClicked', event);
    },

    rowStarToggled: function(event) {
      this.$emit('rowStarToggled', event);
    },

    sort: function(event) {
      const column = this.schema.columns.find(column => column.name == event.sortField);
      if (column) {
        this.loadList({
          filters: this.searchFilters,
          pageSize: this.pageSize,
          orderBy: column.$column.expr,
          orderDirection: event.sortOrder < 0 ? 'desc' : 'asc'
        });
      }
    },

    switchToSummaryView: function() {
      this.$refs.listView.switchToSummaryView();
    },

    switchToTableView: function() {
      this.$refs.listView.switchToTableView();
    },


    _getResultColumn: function(column, idx, sortable) {
      const result = {
        name: 'column.a_' + idx,
        caption: column.caption,
        $column: column,
        sortable: sortable != false && sortable != 'false'
      };

      const mi = column.metainfo;
      if (mi) {
        if (mi.showUnit == 'true') {
          result.type = 'specimen-measure';
          result.measure = mi.measure;
          result.entity = 'hidden';
        } else if (mi.showLink == 'true') {
          if (this.url) {
            result.href = (row) => '' + exprUtil.eval(row.rowObject, this.url)
          }

          if (this.newTab) {
            result.hrefTarget = '_blank';
          }
        }
      }

      return result;
    },

    _isDateFilter: function(name) {
      const filter = this.filters.find(f => f.name == name);
      return filter && filter.type == 'date';
    },

    _toDateString: function(value) {
      if (typeof value == 'string') {
        value = new Date(value);
      }

      return '"' + util.formatDate(value, ui.global.locale.shortDateFmt) + '"';
    }
  }
}
</script>
