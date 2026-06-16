<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'dps.list'">Distribution Protocols</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.dps"
              :page-size="ctx.pageSize"
              :list-size="ctx.dpsCount"
              @updateListSize="getDpsCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <span v-if="!ctx.selectedDps || ctx.selectedDps.length == 0">
                <os-button-link left-icon="plus" :label="$t('common.buttons.create')" :url="createDpUrl"
                  v-show-if-allowed="dpResources.createOpts" />

                <os-menu :label="$t('common.buttons.import')" :options="importOpts"
                  v-show-if-allowed="dpResources.importOpts" />

                <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportDps"
                  v-show-if-allowed="dpResources.importOpts" />

                <os-button-link left-icon="share" :label="$t('dps.view_orders')" :url="ordersUrl"
                  v-show-if-allowed="dpResources.orderOpts" />

                <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                  url="https://openspecimen.atlassian.net/wiki/x/CoHfBg" new-tab="true" />
              </span>

              <span v-else>
                <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteDps"
                  v-show-if-allowed="dpResources.deleteOpts" />

                <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportDps"
                  v-show-if-allowed="dpResources.importOpts" />
              </span>
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.dps"
            :selected="ctx.selectedDp"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="true"
            :loading="ctx.loading"
            @filtersUpdated="loadDps"
            @selectedRows="onDpsSelection"
            @rowClicked="onDpRowClick"
            ref="listView"
          />

          <os-confirm-delete ref="deleteDialog">
            <template #message>
              <span v-t="'dps.confirm_delete_selected'">Are you sure you want to delete the selected distribution protocols?</span>
            </template>
          </os-confirm-delete>
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.dpId > 0 && ctx.selectedDp">
      <router-view :dpId="ctx.selectedDp.dp.id" :key="$route.params.dpId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import exportSvc from '@/common/services/ExportService.js';
import routerSvc from '@/common/services/Router.js';
import dpSvc     from '@/administrative/services/DistributionProtocol.js';

import dpResources from './Resources.js';

export default {
  props: ['filters', 'dpId'],

  data() {
    return {
      ctx: {
        dps: [],
        dpsCount: -1,
        loading: true,
        query: this.filters,

        detailView: false,
        selectedDps: []
      },

      importOpts: [
        {
          icon: 'truck',
          caption: this.$t('dps.list'),
          url: routerSvc.getUrl('DpImportRecords')
        },
        {
          icon: 'list-ol',
          caption: this.$t('dps.requirements'),
          url: routerSvc.getUrl('DpImportRecords', {}, {objectType: 'dpRequirement'})
        },
        {
          icon: 'table',
          caption: this.$t('bulk_imports.view_jobs'),
          url: routerSvc.getUrl('DpImportJobs')
        }
      ],

      dpResources,

      listSchema: { columns: [] },
    };
  },

  created() {
    dpSvc.getListViewSchema().then(listSchema => this.listSchema = listSchema);
  },

  watch: {
    'dpId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.ctx.dps.find(rowObject => rowObject.dp.id == this.dpId);
        if (!selectedRow) {
          selectedRow = {dp: {id: this.dpId}};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newValue == -2);
      }
    }
  },

  computed: {
    createDpUrl: function() {
      return routerSvc.getUrl('DpAddEdit', {dpId: -1});
    },

    ordersUrl: function() {
      return routerSvc.getUrl('OrdersList', {orderId: -1});
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadDps: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const dps = await this.reloadDps();
      if (this.dpId <= 0) {
        routerSvc.goto('DpsList', {dpId: -1}, {filters: uriEncoding});
      } else {
        let selectedRow = dps.find(rowObject => rowObject.dp.id == this.dpId);
        if (!selectedRow) {
          selectedRow = {dp: {id: this.dpId}};
        }

        this.showDetails(selectedRow);
      }
    },

    reloadDps: async function() {
      this.ctx.loading = true;

      const opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      const dps = await dpSvc.getDps(opts);

      this.ctx.loading = false;
      this.ctx.dps = dps.map(dp => ({dp}));
      return this.ctx.dps;
    },

    getDpsCount: async function() {
      this.ctx.dpsCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues);
      const { count } = await dpSvc.getDpsCount(opts);
      this.ctx.dpsCount = count;
    },

    onDpRowClick: function({dp}) {
      routerSvc.goto('DpsListItemDetail.Overview', {dpId: dp.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedDp = rowObject;
      if (!this.ctx.detailView) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('DpsList', {dpId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    onDpsSelection: function(selection) {
      this.ctx.selectedDps = (selection || []).map((row) => row.rowObject.dp);
    },

    exportDps: function() {
      const dpIds = this.ctx.selectedDps.map(dp => dp.id);
      exportSvc.exportRecords({objectType: 'distributionProtocol', recordIds: dpIds});
    },

    deleteDps: function() {
      this.$refs.deleteDialog.open().then(
        async () => {
          const dpIds = this.ctx.selectedDps.map(dp => dp.id);
          const deletedDps = await dpSvc.bulkDelete(dpIds);
          alertsSvc.success({code: 'dps.deleted', args: {count: deletedDps.length}});
          this.$refs.listView.reload();
        }
      );
    }
  }
}
</script>
