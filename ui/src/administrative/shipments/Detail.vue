<template>
  <os-page>
    <os-page-head :noNavButton="noNavButton">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.shipment.name}}</h3>
        <div class="accessories" v-if="ctx.shipment && ctx.shipment.id > 0">
          <os-tag :value="ctx.shipment.status" :rounded="true" :type="tagType" />
          <os-copy-link size="small" :route="{name: 'ShipmentDetail.Overview', params: {shipmentId: ctx.shipment.id}}" />
          <os-new-tab size="small" :route="{name: 'ShipmentDetail.Overview', params: {shipmentId: ctx.shipment.id}}" />
        </div>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="noNavButton">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>
            <li v-show="ctx.shipment.type == 'CONTAINER'">
              <router-link :to="getRoute('Containers')">
                <span v-t="'shipments.containers'">Containers</span>
              </router-link>
            </li>
            <li>
              <router-link :to="getRoute('Specimens')">
                <span v-t="'shipments.specimens'">Specimens</span>
              </router-link>
            </li>

            <os-plugin-views page="shipment-detail" view="tab-menu" />
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

            <li v-if="ctx.shipment.type == 'CONTAINER'">
              <router-link :to="getRoute('Containers')">
                <os-icon name="box-open" />
                <span class="label" v-t="'shipments.containers'">Containers</span>
              </router-link>
            </li>

            <li>
              <router-link :to="getRoute('Specimens')">
                <os-icon name="flask" />
                <span class="label" v-t="'shipments.specimens'">Specimens</span>
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
import i18n         from '@/common/services/I18n.js';
import routerSvc    from '@/common/services/Router.js';
import shipmentSvc  from '@/administrative/services/Shipment.js';

export default {
  props: ['shipmentId', 'noNavButton'],

  data() {
    return {
      ctx: {
        shipment: {},

        bcrumb: [
          {url: routerSvc.getUrl('ShipmentsList', {shipmentId: -1}), label: i18n.msg('shipments.list')}
        ]
      }
    };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.split('.')[0];
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }

    this.loadShipment();
  },

  watch: {
    shipmentId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadShipment();
      }
    }
  },

  computed: {
    tagType: function() {
      switch (this.ctx.shipment.status) {
        case 'Pending':
        case 'Requested':
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
    loadShipment: async function() {
      this.ctx.shipment = await shipmentSvc.getShipment(+this.shipmentId);
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
