<template>
  <os-page>
    <os-page-head :noNavButton="listItemDetailView">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.order.name}}</h3>
        <os-tag :value="status" :rounded="true" :type="tagType" />
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="listItemDetailView">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span>Overview</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Specimens')">
                <span>Specimens</span>
              </router-link>
            </li>

            <os-plugin-views page="order-detail" view="tab-menu" :view-props="{order: ctx.order}" />
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li v-os-tooltip.right="'Overview'">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>

            <li v-os-tooltip.right="'Specimens'">
              <router-link :to="getRoute('Specimens')">
                <os-icon name="flask" />
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

import routerSvc   from '@/common/services/Router.js';
import formUtil    from '@/common/services/FormUtil.js';
import orderSvc    from '@/administrative/services/Order.js';

export default {
  props: ['orderId', 'listItemDetailView'],

  setup() {
    const ctx = reactive({
      order: {},

      bcrumb: [
        {url: routerSvc.getUrl('OrdersList', {orderId: -1}), label: 'Orders'}
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
