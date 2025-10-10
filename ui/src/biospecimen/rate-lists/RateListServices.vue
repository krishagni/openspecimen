<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="addServices" />
      </span>
    </template>
    <template #right>
      <os-list-size :list="ctx.services"  v-if="ctx.services.length > 0" />
    </template>
  </os-page-toolbar>
  <os-grid>
    <os-grid-column width="12">
      <os-list-view
        :data="ctx.services"
        :schema="listSchema"
        :loading="ctx.loading"
        @filtersUpdated="loadServices"
        @rowClicked="onServiceRowClick"
        ref="listView"
      />
    </os-grid-column>
  </os-grid>
</template>

<script>
import rateListSvc from '@/biospecimen/services/RateList.js';
import routerSvc   from '@/common/services/Router.js';

export default {
  props: ['rate-list'],

  data() {
    return {
      ctx: {
        services: [],

        loading: false
      },

      cpUpdateOpts: {resource: 'CollectionProtocol', operations: ['Create', 'Update']},

      listSchema: rateListSvc.getServicesListSchema()
    }
  },

  methods: {
    loadServices: function() {
      this.ctx.loading = true;
      return rateListSvc.getServices(this.rateList.id).then(
        resp => {
          this.ctx.loading = false;
          this.ctx.services = resp.map(service => ({service}));
          return this.ctx.services; 
        }
      );
    },

    onServiceRowClick: function({service}) {
      routerSvc.goto('LabServicesList', {}, {serviceId: service.serviceId});
    }
  }
}
</script>
