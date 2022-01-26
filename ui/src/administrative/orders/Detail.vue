<template>
  <os-page>
    <os-page-head :noNavButton="listItemDetailView">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.order.name}}</h3>
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
import { reactive, watchEffect } from 'vue';

import routerSvc   from '@/common/services/Router.js';
import formUtil    from '@/common/services/FormUtil.js';
import orderSvc    from '@/administrative/services/Order.js';

export default {
  props: ['orderId', 'listItemDetailView'],

  setup(props) {
    const ctx = reactive({
      order: {},

      bcrumb: [
        {url: routerSvc.getUrl('OrdersList', {orderId: -1}), label: 'Orders'}
      ]
    });

    watchEffect(
      async () => {
        ctx.order = await orderSvc.getOrder(+props.orderId);
        formUtil.createCustomFieldsMap(ctx.order, true);
      }
    );

    return { ctx };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }
  },

  methods: {
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
