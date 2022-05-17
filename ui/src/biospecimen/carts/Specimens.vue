<template>
  <os-page>
    <os-page-head :noNavButton="listItemDetailView">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{displayName}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default> </template>

        <template #right>
          <os-list-size
            :list="listInfo.rows"
            :page-size="listInfo.pageSize"
            :list-size="listInfo.size"
            @updateListSize="getSpecimensCount"
          />

          <os-button left-icon="search" label="Search" @click="toggleSearch" />
        </template>
      </os-page-toolbar>

      <os-query-list-view
        name="cart-specimens-list-view"
        :object-id="cartId"
        :allow-selection="true"
        :query="query"
        url="'#/specimens/' + hidden.specimenId"
        @selectedRows="onSpecimenSelection"
        @rowClicked="onSpecimenRowClick"
        @listLoaded="specimensLoaded"
        ref="specimensList"
      />
    </os-page-body>
  </os-page>
</template>

<script>

import cartSvc   from '@/biospecimen/services/SpecimenCart.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cartId', 'query', 'listItemDetailView'],

  data() {
    return {
      ctx: {
        cart: {},

        bcrumb: [
          {url: routerSvc.getUrl('SpecimenCartsList', {cartId: -1}), label: 'Carts'}
        ]
      },

      listInfo: { rows: [], size: 0, pageSize: 0 }
    };
  },

  created() {
    this.loadCart();
  },

  watch: {
    cartId: function(newCartId, oldCartId) {
      if (oldCartId != newCartId) {
        this.loadCart();
      }
    }
  },

  computed: {
    displayName: function() {
      return cartSvc.getDisplayName(this.ctx.cart);
    }
  },

  methods: {
    loadCart: async function() {
      cartSvc.getCart(+this.cartId).then(cart => this.ctx.cart = cart);
    },

    toggleSearch: function() {
      this.$refs.specimensList.toggleShowFilters();
    },

    specimensLoaded: function({widget, list, filters}) {
      this.listInfo = { rows: list.rows, size: widget.size, pageSize: widget.pageSize };

      const query = {...this.$route.query, specimenFilters: filters};
      routerSvc.goto(this.$route.name, this.$route.params, query);
    }
  }
}
</script>
