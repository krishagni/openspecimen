<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>
          <span v-if="query.title">{{query.title}}</span>
          <span v-else v-t="'queries.unsaved_query'">Unsaved Query</span>
        </h3>
      </span>

      <template #right>
        <div class="counters" v-if="ctx.counters">
          <os-card class="counter">
            <template #body>
              <span>
                <os-icon name="users" />
                <span>{{ctx.counters.cprs}}</span>
              </span>
            </template>
          </os-card>
          <os-card class="counter">
            <template #body>
              <span>
                <os-icon name="calendar" />
                <span>{{ctx.counters.visits}}</span>
              </span>
            </template>
          </os-card>
          <os-card class="counter">
            <template #body>
              <span>
                <os-icon name="flask" />
                <span>{{ctx.counters.specimens}}</span>
              </span>
            </template>
          </os-card>
        </div>
      </template>
    </os-page-head>

    <os-page-body>
      <div>
        <AgGridVue class="results-grid" :theme="theme"
          :row-data="ctx.records" :column-defs="ctx.columns"
          :rowSelection="{mode: 'multiRow', headerCheckbox: false}"
          :pinnedBottomRowData="ctx.footerRow"
        />
      </div>

      <os-column-url />
    </os-page-body>
  </os-page>
</template>

<script>
import { themeQuartz } from 'ag-grid-community';
import { AgGridVue }   from 'ag-grid-vue3'

import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';
import querySvc  from '@/queries/services/Query.js';

import ColumnUrl from './ColumnUrl.vue';

export default {
  props: ['query'],

  components: {
    AgGridVue,

    'os-column-url': ColumnUrl
  },

  data() {
    return {
      theme: themeQuartz,

      ctx: {
        bcrumb: [
          {url: routerSvc.getUrl('QueriesList', {}), label: i18n.msg('queries.list')}
        ],

        loadingCounters: false,

        counters: null,

        loadingRecords: false,

        columns: null,

        records: null
      }
    }
  },

  created() {
    this._loadCounters();
    this._loadRecords();
  },

  methods: {
    _loadCounters: async function() {
      this.ctx.loadingCounters = true;
      const {cprs, visits, specimens} = await querySvc.getCount(this.query);

      this.ctx.loadingCounters = false;
      const formatter = new Intl.NumberFormat();
      this.ctx.counters = {
        cprs:      formatter.format(cprs),
        visits:    formatter.format(visits),
        specimens: formatter.format(specimens)
      };
    },

    _loadRecords: async function() {
      this.ctx.loadingRecords = true;
      const {columnLabels, columnMetadata, columnTypes, columnUrls, rows} = await this._getData();

      const {type, params} = this.query.reporting || {type: 'none', params: {}};
      let pinnedColumns = 0;
      if (type == 'crosstab') {
        pinnedColumns = (params.groupRowsBy || []).length;
      }

      this.ctx.columns = columnLabels
        .map((label, idx) => {
          const column = {field: label, name: columnMetadata[idx].expr, pinned: idx < pinnedColumns && 'left'};
          if (columnUrls[idx]) {
            column.cellRenderer = 'os-column-url';
          } else if (columnTypes[idx] == 'DATE') {
            column.valueFormatter = this._formatDate;
          }

          return column;
        })
        .filter(column => column.field.indexOf('$') != 0);

      this.ctx.records = rows.map(
        row =>
          row.reduce(
            (record, value, idx) => {
              record[columnLabels[idx]] = value;
              return record;
            },
            {}
          )
      );

      if (type == 'columnsummary') {
        this.ctx.footerRow = this.ctx.records.splice(this.ctx.records.length - 1, 1);
      } else {
        this.ctx.footerRow = [];
      }

      this.ctx.loadingRecords = false;
    },

    _getData: function() {
      return querySvc.getData(this.query, true, true);
    },

    _formatDate: function(params) {
      if (!params.value) {
        return null;
      } else if (params.value.indexOf('T') != -1) {
        return this.$filters.dateTime(new Date(params.value));
      } else {
        return this.$filters.date(new Date(params.value));
      }
    }
  }
}
</script>

<style scoped>
.counters {
  display: flex;
  flex-direction: row;
  margin-top: -1.25rem;
  opacity: 0.8;
  font-size: 1.25rem;
  font-weight: 700;
}

.counters .counter {
  margin-right: 1rem;
  padding: 0.5rem 1rem;
}

.counters .counter :deep(.os-icon-wrapper) {
  margin-right: 0.5rem;
}

.results-grid {
  width: 100%;
  height: 100%;
}
</style>
