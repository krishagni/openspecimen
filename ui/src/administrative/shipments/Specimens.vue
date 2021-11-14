
<template>
  <div>
    <os-grid>
      <os-grid-column width="12">
        <os-list-view
          :data="ctx.specimens"
          :columns="listSchema.columns"
          :filters="listSchema.filters"
          :query="ctx.query"
          :allowSelection="false"
          :loading="ctx.loading"
          ref="listView">

          <template #footer>
            <os-pager :start-at="ctx.startAt" :have-more="ctx.haveMoreSpmns"
              @previous="previousPage" @next="nextPage" />
          </template>
        </os-list-view>
      </os-grid-column>
    </os-grid>
  </div>
</template>

<script>

import listSchema from '@/administrative/schemas/shipments/specimens.js';

import shipmentSvc from '@/administrative/services/Shipment.js';

const MAX_SPMNS = 50;

export default {
  props: ['shipment'],

  data() {
    return {
      ctx: {
        specimens: [],
        loading: true,
        startAt: 0,
        haveMoreSpmns: false
      },

      listSchema
    }
  },

  created() {
    this.loadSpecimens(0);
  },

  methods: {
    loadSpecimens: async function(startAt) {
      const ctx = this.ctx;
      ctx.loading = true;

      const specimens = await shipmentSvc.getSpecimens(this.shipment.id, {startAt: startAt, maxResults: MAX_SPMNS + 1});
      if (specimens.length > MAX_SPMNS) {
        ctx.haveMoreSpmns = true;
        specimens.splice(specimens.length - 1, 1);
      } else {
        ctx.haveMoreSpmns = false;
      }

      ctx.specimens = specimens;
      ctx.startAt = startAt;
      ctx.loading = false;
    },

    previousPage: function() {
      this.loadSpecimens(this.ctx.startAt - MAX_SPMNS);
    },

    nextPage: function() {
      this.loadSpecimens(this.ctx.startAt + MAX_SPMNS);
    }
  }
}
</script>
