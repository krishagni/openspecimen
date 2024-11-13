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
        <div v-if="ctx.loadingCounters">
          <span v-t="'common.loading'">Loading</span>
        </div>
        <div class="counters" v-else-if="ctx.counters">
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
      <os-page-toolbar>
        <template #default>
          <os-menu :label="$t('queries.actions')" :options="actionsMenuOpts" />

          <span v-if="ctx.showAddSpecimens">
            <os-button left-icon="check" :label="$t('queries.select_all')" @click="selectAllRows"
              v-if="!ctx.allRowsSelected" />

            <os-button left-icon="times" :label="$t('queries.unselect_all')" @click="unselectAllRows"
              v-if="ctx.allRowsSelected" />
          </span>

          <span v-if="ctx.showAddSpecimens && ctx.selectedRows.length > 0">
            <os-specimen-actions label="queries.specimen_actions"
              :specimens="selectedSpecimens" @reloadSpecimens="rerun" />

            <os-add-to-cart :specimens="selectedSpecimens" />
          </span>
        </template>

        <template #right v-if="selectedSpecimens.length > 0">
          <os-message class="selected-rows-msg" type="info">
            <span v-t="{path: 'queries.specimens_selected', args: {count: selectedSpecimens.length}}" />
          </os-message>
        </template>
      </os-page-toolbar>

      <os-grid>
        <os-grid-column style="flex: 0;">
          <Facets :query="query" @facets-selected="onFacetsSelection" />
        </os-grid-column>

        <os-grid-column>
          <os-message type="info" v-if="ctx.loadingRecords">
            <span v-t="'queries.loading_records'">Loading records...</span>
          </os-message>

          <AgGridVue class="results-grid" :theme="theme"
            :row-data="ctx.records" :column-defs="ctx.columns"
            :rowSelection="rowSelection" :pinnedBottomRowData="ctx.footerRow"
            @gridReady="onGridReady" @selectionChanged="onRowSelection"
            v-else />
        </os-grid-column>
      </os-grid>

      <os-column-url />
    </os-page-body>
  </os-page>

  <SaveQuery ref="saveQueryDialog" />

  <DefineView ref="defineViewDialog" />
</template>

<script>
import { themeQuartz } from 'ag-grid-community';
import { AgGridVue }   from 'ag-grid-vue3'

import alertsSvc from '@/common/services/Alerts.js';
import authSvc   from '@/common/services/Authorization.js';
import i18n      from '@/common/services/I18n.js';
import querySvc  from '@/queries/services/Query.js';
import routerSvc from '@/common/services/Router.js';

import queryResources from './Resources.js';

import ColumnUrl from './ColumnUrl.vue';
import DefineView from './DefineView.vue';
import Facets from './Facets.vue';
import SaveQuery from './SaveQuery.vue';

export default {
  props: ['query'],

  emits: ['query-saved'],

  components: {
    AgGridVue,

    'os-column-url': ColumnUrl,

    Facets,

    DefineView,

    SaveQuery
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

        records: null,

        showAddSpecimens: false,

        allRowsSelected: false,

        selectedRows: []
      }
    }
  },

  mounted() {
    this._loadCounters();

    if (!this.query.selectList || this.query.selectList.length == 0) {
      this.showDefineViewDialog();
    } else {
      this._loadRecords();
    }
  },

  computed: {
    actionsMenuOpts: function() {
      const options = [];
      if (authSvc.isAllowed(queryResources.updateOpts)) {
        options.push({icon: 'edit', caption: this.$t('common.buttons.edit'), onSelect: () => this.editQuery()});
      }

      if (authSvc.isAllowed(queryResources.createOpts) || authSvc.isAllowed(queryResources.updateOpts)) {
        options.push({icon: 'save', caption: this.$t('common.buttons.save'), onSelect: () => this.showSaveQueryDialog()});
      }

      options.push({icon: 'columns', caption: this.$t('queries.columns'), onSelect: () => this.showDefineViewDialog()});
      options.push({icon: 'redo', caption: this.$t('queries.rerun'), onSelect: () => this.rerun()});

      if (authSvc.isAllowed(queryResources.importOpts)) {
        options.push({icon: 'download', caption: this.$t('common.buttons.export'), onSelect: () => this.exportQueryData()});
      }

      return options;
    },

    rowSelection: function() {
      if (!this.ctx.showAddSpecimens) {
        return null;
      }

      return {mode: 'multiRow', headerCheckbox: false, enableClickSelection: false, checkboxes: true};
    },

    selectedSpecimens: function() {
      return this.ctx.selectedRows.map(row => ({id: +row['$specimenId'], cpId: +row['$cpId']}));
    }
  },

  methods: {
    editQuery: function() {
      if (this.query.id > 0) {
        routerSvc.goto('QueryDetail.AddEdit', {queryId: this.query.id});
      } else {
        routerSvc.back();
      }
    },

    showSaveQueryDialog: function() {
      this.$refs.saveQueryDialog.save(this.query).then(
        ({status, query}) => {
          if (status != 'saved') {
            return;
          }

          alertsSvc.success({code: 'queries.saved', args: query});
          this.$emit('query-saved', query);
        }
      );
    },

    rerun: function() {
      this._loadCounters();
      this._loadRecords();
    },

    showDefineViewDialog: function() {
      this.$refs.defineViewDialog.open(this.query).then(
        resp => {
          if (resp == 'cancel') {
            return;
          }

          const {selectedFields: selectList, havingClause, reporting} = resp;
          this.$emit('query-saved', {...this.query, selectList, havingClause, reporting});
          setTimeout(() => this._loadRecords()); // to allow the query to be updated
        }
      );
    },

    exportQueryData: function() {
      querySvc.exportData(this.query);
    },

    onGridReady: function({api}) {
      this.ctx.api = api;
    },

    onRowSelection: function({api}) {
      const {ctx} = this;
      ctx.selectedRows = api.getSelectedRows();
      ctx.allRowsSelected = (ctx.selectedRows.length == ctx.records.length)
    },

    selectAllRows: function() {
      this.ctx.api.selectAll();
      this.ctx.allRowsSelected = true;
    },

    unselectAllRows: function() {
      this.ctx.api.deselectAll();
      this.ctx.allRowsSelected = false;
    },

    onFacetsSelection: function(selectedFacets) {
      const facets = selectedFacets.map(({facet, values}) => ({id: facet.id, type: facet.type, values}));
      this._loadCounters(facets);
      this._loadRecords(facets);
    },

    _loadCounters: async function(facets) {
      this.ctx.loadingCounters = true;
      const {cprs, visits, specimens} = await querySvc.getCount(this.query, facets);

      this.ctx.loadingCounters = false;
      const formatter = new Intl.NumberFormat();
      this.ctx.counters = {
        cprs:      formatter.format(cprs),
        visits:    formatter.format(visits),
        specimens: formatter.format(specimens)
      };
    },

    _loadRecords: async function(facets) {
      this.ctx.loadingRecords = true;
      const {columnLabels, columnMetadata, columnTypes, columnUrls, rows} = await this._getData(facets);

      const {type, params} = this.query.reporting || {type: 'none', params: {}};
      let pinnedColumns = 0;
      if (type == 'crosstab') {
        pinnedColumns = (params.groupRowsBy || []).length;
      }

      this.ctx.columns = columnLabels
        .map((label, idx) => {
          const column = {field: label, name: columnMetadata[idx].expr, pinned: idx < pinnedColumns && 'left'};
          column.headerName = label.substring(label.lastIndexOf('#') + 1);
          column.url = columnUrls[idx];

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

      this.ctx.allRowsSelected = false;
      this.ctx.selectedRows = [];

      if (type == 'columnsummary') {
        this.ctx.footerRow = this.ctx.records.splice(this.ctx.records.length - 1, 1);
      } else {
        this.ctx.footerRow = [];
      }

      this.ctx.showAddSpecimens = false;
      if (type != 'crosstab' && columnMetadata && columnMetadata.length) {
        for (const {expr} of columnMetadata) {
          if (expr == 'Specimen.label' || expr == 'Specimen.barcode') {
            this.ctx.showAddSpecimens = true;
            break;
          }
        }
      }

      this.ctx.loadingRecords = false;
    },

    _getData: function(facets) {
      return querySvc.getData(this.query, facets, true, true);
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

.selected-rows-msg {
  margin: 0;
}

.selected-rows-msg :deep(.p-message-wrapper) {
  padding: 0.25rem;
}
</style>
