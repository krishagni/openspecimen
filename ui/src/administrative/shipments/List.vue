<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'shipments.list'">Shipments</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.shipments"
              :page-size="ctx.pageSize"
              :list-size="ctx.shipmentsCount"
              @updateListSize="getShipmentsCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <os-menu :label="$t('common.buttons.create')" :options="createOptions"
                v-show-if-allowed="shipmentResources.createOpts" />

              <os-menu :label="$t('common.buttons.import')" :options="importOpts"
                v-show-if-allowed="shipmentResources.importOpts" />

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://help.openspecimen.org/shipment" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.shipments"
            :selected="ctx.selectedShipment"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="false"
            :loading="ctx.loading"
            @filtersUpdated="loadShipments"
            @rowClicked="selectShipment"
            ref="listView"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.shipmentId > 0 && ctx.selectedShipment">
      <router-view :shipmentId="ctx.selectedShipment.shipment.id" :key="$route.params.shipmentId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import routerSvc   from '@/common/services/Router.js';
import shipmentSvc from '@/administrative/services/Shipment.js';

import shipmentResources from './Resources.js';

export default {
  props: ['filters', 'shipmentId'],

  data() {
    return {
      ctx: {
        shipments: [],
        shipmentsCount: -1,
        loading: true,
        query: this.filters,

        detailView: false
      },

      listSchema: { columns: [] },

      createOptions: [
        {
          icon: 'flask',
          caption: this.$t('shipments.specimens'),
          onSelect: () => routerSvc.goto('ShipmentAddEdit', {shipmentId: -1})
        },

        {
          icon: 'box-open',
          caption: this.$t('shipments.containers'),
          onSelect: () => routerSvc.goto('ShipmentAddEdit', {shipmentId: -1}, {shipmentType: 'CONTAINER'})
        }
      ],

      importOpts: [
        {
          icon: 'flask',
          caption: this.$t('shipments.specimen_shipments'),
          onSelect: () => routerSvc.goto('ShipmentImportRecords', {}, {objectType: 'shipment'})
        },
        {
          icon: 'box-open',
          caption: this.$t('shipments.container_shipments'),
          onSelect: () => routerSvc.goto('ShipmentImportRecords', {}, {objectType: 'containerShipment'})
        },
        {
          icon: 'table',
          caption: this.$t('bulk_imports.view_jobs'),
          onSelect: () => routerSvc.goto('ShipmentImportJobs')
        }
      ],

      shipmentResources,
    };
  },

  created() {
    shipmentSvc.getListViewSchema().then(listSchema => this.listSchema = listSchema);
  },

  watch: {
    'shipmentId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.ctx.shipments.find(rowObject => rowObject.shipment.id == this.shipmentId);
        if (!selectedRow) {
          selectedRow = {shipment: {id: this.shipmentId}};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newValue == -2);
      }
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadShipments: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const shipments = await this.reloadShipments();
      if (this.shipmentId <= 0) {
        routerSvc.goto('ShipmentsList', {shipmentId: -1}, {filters: uriEncoding});
      } else {
        let selectedRow = shipments.find(rowObject => rowObject.shipment.id == this.shipmentId);
        if (!selectedRow) {
          selectedRow = {shipment: {id: this.shipmentId}};
        }

        this.showDetails(selectedRow);
      }
    },

    reloadShipments: function() {
      this.ctx.loading = true;
      let opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      return shipmentSvc.getShipments(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.shipments = resp.map(shipment => ({shipment: shipment}));
        return this.ctx.shipments;
      });
    },

    getShipmentsCount: function() {
      this.ctx.shipmentsCount = -1;
      let opts = Object.assign({}, this.ctx.filterValues);
      shipmentSvc.getShipmentsCount(opts).then(resp => this.ctx.shipmentsCount = resp.count);
    },

    selectShipment: function(rowObject) {
      routerSvc.goto('ShipmentsListItemDetail.Overview', {shipmentId: rowObject.shipment.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedShipment = rowObject;
      if (!this.ctx.detailView) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('ShipmentsList', {shipmentId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    }
  }
}
</script>
