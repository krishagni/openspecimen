<template>
  <div>
    <os-grid>
      <os-grid-column width="12">
        <os-list-view
          :data="ctx.containers"
          :schema="listSchema"
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

import { reactive } from 'vue';

import listSchema  from '@/administrative/schemas/shipments/containers.js';
import shipmentSvc from '@/administrative/services/Shipment.js';

export default {
  props: ['shipment'],

  setup(props) {
    const ctx = reactive({
      containers: [],
      loading: true
    });

    ctx.loading = true;
    shipmentSvc.getContainers(props.shipment.id).then(
      (containers) => {
        ctx.loading = false;
        ctx.containers = containers;
      }
    );

    return { ctx, listSchema };
  },
}
</script>
