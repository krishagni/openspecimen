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
          ref="listView">
          <template #footer>
            <os-pager :start-at="ctx.startAt" :have-more="ctx.haveMoreContainers"
              @previous="previousPage" @next="nextPage" />
          </template>
        </os-list-view>
      </os-grid-column>
    </os-grid>
  </div>
</template>

<script>
import listSchema  from '@/administrative/schemas/shipments/containers.js';
import shipmentSvc from '@/administrative/services/Shipment.js';

const MAX_CONTAINERS = 25;

export default {
  props: ['shipment'],

  data() {
    return {
      ctx: {
        containers: [],
        loading: true,
        startAt: 0,
        haveMoreContainers: false
      },

      listSchema
    }
  },

  created() {
    this.loadContainers(0);
  },


  methods: {
    loadContainers: async function(startAt) {
      const ctx = this.ctx;
      ctx.loading = true;

      const containers = await shipmentSvc.getContainers(
        this.shipment.id, {startAt: startAt, maxResults: MAX_CONTAINERS + 1});
      if (containers.length > MAX_CONTAINERS) {
        ctx.haveMoreContainers = true;
        containers.splice(containers.length - 1, 1);
      } else {
        ctx.haveMoreContainers = false;
      }

      ctx.containers = containers;
      ctx.startAt = startAt;
      ctx.loading = false;
    },

    previousPage: function() {
      this.loadContainers(this.ctx.startAt - MAX_CONTAINERS);
    },

    nextPage: function() {
      this.loadContainers(this.ctx.startAt + MAX_CONTAINERS);
    }
  }
}
</script>
