<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span class="os-title">
        <h3>{{ctx.shipment.name}}</h3>
        <os-tag :value="ctx.shipment.status" :rounded="true" :type="tagType" />
      </span>
    </os-page-head>
    <os-page-body>
      <os-side-menu>
        <ul>
          <li>
            <router-link :to="{name: 'ShipmentOverview'}">
              <os-icon name="eye" />
            </router-link>
          </li>

          <li v-if="ctx.shipment.type == 'CONTAINER'">
            <router-link :to="{name: 'ShipmentContainers'}">
              <os-icon name="box-open" />
            </router-link>
          </li>

          <li>
            <router-link :to="{name: 'ShipmentSpecimens'}">
              <os-icon name="flask" />
            </router-link>
          </li>

          <os-plugin-views page="shipment-detail" view="side-menu" />
        </ul>
      </os-side-menu>

      <router-view :shipment="ctx.shipment" v-if="ctx.shipment.id" />
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, watchEffect } from 'vue';

import routerSvc    from '@/common/services/Router.js';
import shipmentSvc  from '@/administrative/services/Shipment.js';

export default {
  props: ['shipmentId'],

  setup(props) {
    let ctx = reactive({
      shipment: {},

      bcrumb: [
        {url: routerSvc.getUrl('ShipmentsList'), label: 'Shipments'}
      ]
    });

    watchEffect(
      async () => {
        ctx.shipment = await shipmentSvc.getShipment(+props.shipmentId);
      }
    );

    return { ctx };
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
  }
}
</script>
