<template>
  <os-page>
    <os-page-head>
      <span>
        <h3>Shipments</h3>
      </span>

      <template #right>
        <os-list-size
          :list="ctx.shipments"
          :page-size="ctx.pageSize"
          :list-size="ctx.shipmentsCount"
          @updateListSize="getShipmentsCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-menu label="Create" :options="createOptions" v-show-if-allowed="shipmentResources.createOpts" />

          <os-menu label="Import" :options="importOpts" v-show-if-allowed="shipmentResources.importOpts" />

          <os-button-link left-icon="question-circle" label="Help"
            url="https://help.openspecimen.org/shipment" new-tab="true" />
        </template>

        <template #right>
          <os-button left-icon="search" label="Search" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.shipments"
        :columns="listSchema.columns"
        :filters="listSchema.filters"
        :query="ctx.query"
        :allowSelection="false"
        :loading="ctx.loading"
        @filtersUpdated="loadShipments"
        @rowClicked="showDetails"
        ref="listView"
      />
    </os-page-body>
  </os-page>
</template>

<script>

import routerSvc   from '@/common/services/Router.js';
import shipmentSvc from '@/administrative/services/Shipment.js';

import shipmentResources from './Resources.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        shipments: [],
        shipmentsCount: -1,
        loading: true,
        query: this.filters
      },

      listSchema: { columns: [] },

      createOptions: [
        {
          icon: 'flask',
          caption: 'Specimens',
          onSelect: () => routerSvc.goto('ShipmentAddEdit', {shipmentId: -1})
        },

        {
          icon: 'box-open',
          caption: 'Containers',
          onSelect: () => routerSvc.goto('ShipmentAddEdit', {shipmentId: -1}, {shipmentType: 'CONTAINER'})
        }
      ],

      importOpts: [
        {
          icon: 'flask',
          caption: 'Specimen Shipments',
          onSelect: () => routerSvc.ngGoto('shipment-import')
        },
        {
          icon: 'box-open',
          caption: 'Container Shipments',
          onSelect: () => routerSvc.ngGoto('shipment-import', {type: 'containerShipment'})
        },
        {
          icon: 'table',
          caption: 'View Past Imports',
          onSelect: () => routerSvc.ngGoto('shipment-import-jobs')
        }
      ],

      shipmentResources,
    };
  },

  created() {
    shipmentSvc.getListViewSchema().then(listSchema => this.listSchema = listSchema);
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadShipments: function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      routerSvc.goto('ShipmentsList', {}, {filters: uriEncoding});
      this.reloadShipments();
    },

    reloadShipments: function() {
      this.ctx.loading = true;
      let opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      shipmentSvc.getShipments(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.shipments = resp.map(shipment => ({shipment: shipment}));
      });
    },

    getShipmentsCount: function() {
      this.ctx.shipmentsCount = -1;
      let opts = Object.assign({}, this.ctx.filterValues);
      shipmentSvc.getShipmentsCount(opts).then(resp => this.ctx.shipmentsCount = resp.count);
    },

    showDetails: function({shipment}) {
      this.$goto('ShipmentOverview', {shipmentId: shipment.id});
    }
  }
}
</script>
