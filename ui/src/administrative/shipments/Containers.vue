
<template>
  <div>
    <os-grid>
      <os-grid-column width="12">
        <os-list-view
          :data="ctx.containers"
          :columns="listSchema.columns"
          :filters="listSchema.filters"
          :query="ctx.query"
          :allowSelection="false"
          :loading="ctx.loading"
          ref="listView"
        />
      </os-grid-column>
    </os-grid>
  </div>
</template>

<script>

import { reactive, watchEffect } from 'vue';

import listSchema from '@/administrative/schemas/shipments/containers.js';
import shipmentSvc from '@/administrative/services/Shipment.js';

export default {
  props: ['shipment'],

  setup(props) {
    const ctx = reactive({
      containers: [],
      loading: true
    });

    let lastShipmentId = -1;
    watchEffect(
      () => {
        if (lastShipmentId == props.shipment.id) {
          return;
        }

        ctx.loading = true;
        shipmentSvc.getContainers(props.shipment.id).then(
          (containers) => {
            ctx.loading = false;
            ctx.containers = containers;
            lastShipmentId = props.shipment.id;
          }
        );
      }
    );

    return { ctx, listSchema };
  }
}
</script>
