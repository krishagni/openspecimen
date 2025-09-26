<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'orders.list'">Orders</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.orders"
              :page-size="ctx.pageSize"
              :list-size="ctx.ordersCount"
              @updateListSize="getOrdersCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <os-button left-icon="plus" :label="$t('common.buttons.create')"
                @click="createOrder" v-show-if-allowed="orderResources.createOpts" />

              <os-menu :label="$t('common.buttons.import')" :options="importOpts"
                v-show-if-allowed="orderResources.importOpts" />

              <os-button left-icon="undo" :label="$t('orders.return_specimens')"
                @click="returnSpecimens" v-show-if-allowed="orderResources.updateOpts" />

              <os-button left-icon="truck" :label="$t('orders.view_dps')"
                @click="viewDps" v-show-if-allowed="orderResources.dpOpts" />

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://openspecimen.atlassian.net/wiki/x/FQD1W" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.orders"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="false"
            :loading="ctx.loading"
            :selected="ctx.selectedOrder"
            @filtersUpdated="loadOrders"
            @rowClicked="onOrderRowClick"
            ref="listView"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.orderId > 0 && ctx.selectedOrder">
      <router-view :orderId="ctx.selectedOrder.order.id" :key="$route.params.orderId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import listSchema from '@/administrative/schemas/orders/list.js';

import orderSvc    from '@/administrative/services/Order.js';
import routerSvc   from '@/common/services/Router.js';

import orderResources from './Resources.js';

export default {
  props: ['orderId', 'filters'],

  data() {
    return {
      ctx: {
        orders: [],
        ordersCount: -1,
        loading: true,
        query: this.filters,
        detailView: false,
        selectedOrder: null
      },

      listSchema,

      orderResources,

      importOpts: [
        {
          icon: 'share',
          caption: this.$t('orders.list'),
          onSelect: () => routerSvc.goto('OrderImportRecords')
        },
        {
          icon: 'undo',
          caption: this.$t('orders.return_specimens'),
          onSelect: () => routerSvc.goto('OrderImportRecords', {}, {objectType: 'returnSpecimen'})
        },
        {
          icon: 'table',
          caption: this.$t('bulk_imports.view_jobs'),
          onSelect: () => routerSvc.goto('OrderImportJobs')
        }
      ],
    };
  },

  watch: {
    'orderId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.ctx.orders.find(rowObject => rowObject.order.id == this.orderId);
        if (!selectedRow) {
          selectedRow = {order: {id: this.orderId}};
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

    loadOrders: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const orders = await this.reloadOrders();
      if (this.orderId <= 0) {
        routerSvc.goto('OrdersList', {orderId: -1}, {filters: uriEncoding});
      } else {
        let selectedRow = orders.find(rowObject => rowObject.order.id == this.orderId);
        if (!selectedRow) {
          selectedRow = {order: {id: this.orderId}};
        }

        this.showDetails(selectedRow);
      }
    },

    reloadOrders: async function() {
      this.ctx.loading = true;
      const opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      const orders = await orderSvc.getOrders(opts);
      this.ctx.loading = false;
      this.ctx.orders = orders.map(order => ({order: order}));
      return this.ctx.orders;
    },

    getOrdersCount: function() {
      this.ctx.ordersCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues);
      orderSvc.getOrdersCount(opts).then((resp) => this.ctx.ordersCount = resp.count);
    },

    onOrderRowClick: function(rowObject) {
      routerSvc.goto('OrdersListItemDetail.Overview', {orderId: rowObject.order.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedOrder = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('OrdersList', {orderId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    createOrder: function() {
      routerSvc.goto('OrderAddEdit', {orderId: -1});
    },

    returnSpecimens: function() {
      routerSvc.goto('OrderReturnSpecimens');
    },

    viewDps: function() {
      routerSvc.goto('DpsList', {dpId: -1});
    }
  }
}
</script>
