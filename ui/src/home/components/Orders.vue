<template>
  <os-home-list-card :icon="'share'" :title="$t('common.home.orders')"
    :show-star="false" :list-url="{name: 'OrdersList', params: {orderId: -1}}" :list="ctx.orders"
    @search="search($event)" />
</template>

<script>

import orderSvc from '@/administrative/services/Order.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['widget'],

  data() {
    return {
      ctx: {
        orders: [],

        defList: null,

        search: null
      }
    };
  },

  created() {
    this._loadOrders();
  },

  methods: {
    search: function({searchTerm}) {
      this.ctx.search = searchTerm;
      this._loadOrders(searchTerm);
    },

    _loadOrders: function(searchTerm) {
      if (!searchTerm && this.ctx.defList) {
        this.ctx.orders = this.ctx.defList;
        return;
      }

      orderSvc.getOrders({query: searchTerm, orderByStarred: true, maxResults: 25}).then(
        (orders) => {
          this.ctx.orders = orders;
          if (!searchTerm) {
            this.ctx.defList = orders;
          }

          orders.forEach(
            order => {
              order.displayName = '#' + order.id + ' ' + order.name;
              order.url = routerSvc.getUrl('OrdersListItemDetail.Overview', {orderId: order.id});
            }
          );
        }
      );
    }
  }
}
</script>
