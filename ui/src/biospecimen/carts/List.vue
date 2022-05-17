<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3>Carts</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="'Switch to table view'"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.carts"
              :page-size="ctx.pageSize"
              :list-size="ctx.cartsCount"
              @updateListSize="getCartsCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <os-button left-icon="plus" label="Create" @click="createCart" />

              <os-button label="View My Default Cart" @click="viewDefaultCart" />

              <os-button-link left-icon="question-circle" label="Help"
                url="https://help.openspecimen.org/specimen-list" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" label="Search" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.carts"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="false"
            :loading="ctx.loading"
            :selected="ctx.selectedCart"
            @filtersUpdated="loadCarts"
            @rowClicked="onCartRowClick"
            @rowStarToggled="onToggleStar"
            ref="listView"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.cartId > 0 && ctx.selectedCart">
      <router-view :cartId="ctx.selectedCart.cart.id" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import listSchema from  '@/biospecimen/schemas/carts/list.js';

import cartSvc     from '@/biospecimen/services/SpecimenCart.js';
import alertSvc    from '@/common/services/Alerts.js';
import routerSvc   from '@/common/services/Router.js';

export default {
  props: ['cartId', 'filters'],

  data() {
    return {
      ctx: {
        carts: [],
        cartsCount: -1,
        loading: true,
        query: this.filters,
        detailView: false,
        selectedCart: null
      },

      listSchema,
    };
  },

  watch: {
    'cartId': function(newCartId, oldCartId) {
      if (newCartId == oldCartId) {
        return;
      }

      if (newCartId > 0) {
        let selectedRow = this.ctx.carts.find(({cart}) => cart.id == newCartId);
        if (!selectedRow) {
          selectedRow = {cart: {id: newCartId}};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newCartId == -2);
      }
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadCarts: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const carts = await this.reloadCarts();
      if (this.cartId <= 0) {
        routerSvc.goto('SpecimenCartsList', {cartId: -1}, {filters: uriEncoding});
      } else {
        let selectedRow = carts.find(({cart}) => cart.id == this.cartId);
        if (!selectedRow) {
          selectedRow = {cart: {id: this.cartId}};
        }

        this.showDetails(selectedRow);
      }
    },

    reloadCarts: async function() {
      this.ctx.loading = true;

      const defOpts = {orderByStarred: true, includeStats: true, maxResults: this.ctx.pageSize}; 
      const opts    = Object.assign(defOpts, this.ctx.filterValues || {});
      const carts   = await cartSvc.getCarts(opts);

      this.ctx.loading = false;
      this.ctx.carts = carts.map(cart => ({cart: cart}));
      return this.ctx.carts;
    },

    getCartsCount: function() {
      this.ctx.cartsCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues);
      cartSvc.getCartsCount(opts).then(({count}) => this.ctx.cartsCount = count);
    },

    onCartRowClick: function({cart}) {
      routerSvc.goto('CartSpecimensList', {cartId: cart.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedCart = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('SpecimenCartsList', {cartId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    createCart: function() {
      alertSvc.underDev();
    },

    viewDefaultCart: function() {
      alertSvc.underDev();
    },

    onToggleStar: async function({cart}) {
      let resp;
      if (cart.starred) {
        resp = await cartSvc.unstar(cart);
      } else {
        resp = await cartSvc.star(cart);
      }

      if (resp.status) {
        cart.starred = !cart.starred;
      }
    },
  }
}
</script>
