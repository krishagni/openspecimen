<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.shipment.name}}</h3>
        <os-tag :value="ctx.shipment.status" :rounded="true" :type="tagType" />
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="noNavButton">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span>Overview</span>
              </router-link>
            </li>
            <li v-show="ctx.shipment.type == 'CONTAINER'">
              <router-link :to="getRoute('Containers')">
                <span>Containers</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('Specimens')">
                <span>Specimens</span>
              </router-link>
            </li>

            <os-plugin-views page="shipment-detail" view="tab-menu" />
          </ul>
        </os-tab-menu>

        <os-side-menu v-else>
          <ul>
            <li v-os-tooltip.right="'Overview'">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>

            <li v-if="ctx.shipment.type == 'CONTAINER'" v-os-tooltip.right="'Containers'">
              <router-link :to="getRoute('Containers')">
                <os-icon name="box-open" />
              </router-link>
            </li>

            <li v-os-tooltip.right="'Specimens'">
              <router-link :to="getRoute('Specimens')">
                <os-icon name="flask" />
              </router-link>
            </li>

            <os-plugin-views page="shipment-detail" view="side-menu" />
          </ul>
        </os-side-menu>

        <router-view :shipment="ctx.shipment" v-if="ctx.shipment.id" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, watchEffect } from 'vue';

import routerSvc    from '@/common/services/Router.js';
import shipmentSvc  from '@/administrative/services/Shipment.js';

export default {
  props: ['shipmentId', 'noNavButton'],

  setup(props) {
    let ctx = reactive({
      shipment: {},

      bcrumb: [
        {url: routerSvc.getUrl('ShipmentsList', {shipmentId: -1}), label: 'Shipments'}
      ]
    });

    watchEffect(
      async () => {
        ctx.shipment = {};
        ctx.shipment = await shipmentSvc.getShipment(+props.shipmentId);
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

  computed: {
    tagType: function() {
      switch (this.ctx.shipment.status) {
        case 'Pending':
          return 'warning';
        case 'Shipped':
          return 'info';
        case 'Received':
          return 'success';
      }

      return 'danger';
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
