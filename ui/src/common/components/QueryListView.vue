<template>
  <os-list-view
    :schema="schema"
    :data="data"
    :allowSelection="allowSelection"
    :selected="selected"
    :query="query"
    @filtersUpdated="loadList"
    @selectedRows="onRowsSelection"
    @rowClicked="rowClicked"
    @sort="sort"
    ref="listView"
  />
</template>

<script>

import ui       from '@/global.js';
import exprUtil from '@/common/services/ExpressionUtil.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';

export default {
  props: ['name', 'objectId', 'url', 'newTab', 'allowSelection', 'selected', 'query', 'autoSearchOpen'],

  emits: ['selectedRows', 'rowClicked', 'listLoaded'],

  data() {
    return {
      config: {},

      list: {},

      pageSize: undefined,

      size: undefined, // size of actual list

      schema: {},

      data: [],

      selectedSpecimens: []
    };
  },

  async created() {
    const listId = this.listId = {listName: this.name, objectId: this.objectId};

    const cache = {};
    this.config = await http.get('lists/config', listId);

    this.filters = (this.config.filters || []).map(
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
        }

        return result;
      }
    );
  },

  watch: {
    'objectId': function(newVal, oldVal) {
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
          if (!filters[filter]) {
            return;
          }

          if (filter.endsWith('.$min') || filter.endsWith('.$max')) {
            const key = filter.substr(0, filter.lastIndexOf('.'));
            rangeFilters[key] = rangeFilters[key] || [];
            if (filter.endsWith('.$min')) {
              rangeFilters[key][0] = filters[filter];
            } else {
              rangeFilters[key][1] = filters[filter];
            }
          } else {
            let values = filters[filter];
            if (!(values instanceof Array)) {
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

      this.list = await http.post(
        'lists/data',
        this.search,
        {
          ...this.listId,
          includeCount: this.includeCount,
          orderBy: orderBy,
          orderDirection: orderDirection,
          maxResults: pageSize
        }
      );
      
      this.schema = {filters: this.filters};
      const columns = this.schema.columns = this.list.columns.map(
        (column, idx) => {
          const result = {
            name: 'column.a_' + idx,
            caption: column.caption,
            $column: column,
            sortable: true
          };

          const mi = column.metainfo;
          if (mi) {
            if (mi.showUnit == 'true') {
              result.type = 'specimen-measure';
              result.measure = mi.measure;
              result.entity = 'hidden';
            } else if (mi.showLink == 'true' && this.url) {
              result.href = (row) => ui.ngServer + exprUtil.eval(row.rowObject, this.url)
              if (this.newTab) {
                result.hrefTarget = '_blank';
              }
            }
          }

          return result;
        }
      );

      const urlColumnIdx = columns.findIndex(column => column.href);
      const urlColumn = urlColumnIdx >= 0 ? columns[urlColumnIdx] : null;
      if (urlColumn) {
        this.schema.summary = {
          title: {
            text: (ro) => exprUtil.eval(ro, urlColumn.name),
            url: (ro) => ui.ngServer + exprUtil.eval(ro, this.url)
          },
          descriptions: columns.map(column => column.name).filter((column, index) => index != urlColumnIdx)
        };
      }

      this.data = this.list.rows.map(
        (row) => {
          const columnData = row.data.reduce((acc, item, idx) => { acc['a_' + idx] = item; return acc; }, {});
          return {
            column: columnData,
            hidden: row.hidden
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

      const hasFilters = this.hasFilters() && Object.values(filters || {}).length == 0;
      if (this.autoSearchOpen == true && this.data.length > 12 && hasFilters) {
        this.$refs.listView.displayFilters();
      }

      const fb = util.uriEncode(filters || {});
      this.$emit('listLoaded', {widget: this, list: this.list, filters: fb});
    },

    reload: function() {
      this.$refs.listView.reload();
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
    }
  }
}
</script>
