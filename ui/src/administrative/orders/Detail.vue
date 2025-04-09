<template>
  <os-page>
    <os-page-head :noNavButton="listItemDetailView">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.order.name}}</h3>
        <div class="accessories" v-if="ctx.order && ctx.order.id > 0">
          <os-tag :value="status" :rounded="true" :type="tagType" />
          <os-copy-link size="small" :route="{name: 'OrderDetail.Overview', params: {orderId: ctx.order.id}}" />
          <os-new-tab size="small" :route="{name: 'OrderDetail.Overview', params: {orderId: ctx.order.id}}" />
        </div>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="listItemDetailView">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Specimens')">
                <span v-t="'orders.specimens'">Specimens</span>
              </router-link>
            </li>

            <os-plugin-views page="order-detail" view="tab-menu" :view-props="{order: ctx.order}" />
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
                <span class="label" v-t="'common.overview'">Overview</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Specimens')">
                <os-icon name="flask" />
                <span class="label" v-t="'orders.specimens'">Overview</span>
              </router-link>
            </li>

            <os-plugin-views page="order-detail" view="side-menu" :view-props="{order: ctx.order}" />
          </ul>
        </os-side-menu>

        <router-view :order="ctx.order" v-if="ctx.order.id"> </router-view>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive } from 'vue';

import i18n        from '@/common/services/I18n.js';
import routerSvc   from '@/common/services/Router.js';
import formUtil    from '@/common/services/FormUtil.js';
import orderSvc    from '@/administrative/services/Order.js';

export default {
  props: ['orderId', 'listItemDetailView'],

  setup() {
    const ctx = reactive({
      order: {},

      bcrumb: [
        {url: routerSvc.getUrl('OrdersList', {orderId: -1}), label: i18n.msg('orders.list')}
      ]
    });

    return { ctx };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }

    this.loadOrder();
  },

  watch: {
    orderId: function(newVal, oldVal) {
      if (oldVal != newVal) {
        this.loadOrder();
      }
    }
  },

  computed: {
    tagType: function() {
      switch (this.ctx.order.status) {
        case 'PENDING':
          return 'warning';
        case 'EXECUTED':
          return 'info';
      }

      return '';
    },

    status: function() {
      switch (this.ctx.order.status) {
        case 'PENDING':
          return 'Pending';
        case 'EXECUTED':
          return 'Executed';
      }

      return '';
    }
  },

  methods: {
    loadOrder: async function() {
      this.ctx.order = await orderSvc.getOrder(+this.orderId);
      formUtil.createCustomFieldsMap(this.ctx.order, true);
    },

    getRoute: function(routeName, params, query) {
      return {
        name: this.detailRouteName + '.' + routeName,
        params: params,
        query: {...this.query, query}
      }
    }
  }
}
</script>
