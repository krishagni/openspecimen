<template>
  <os-page>
    <os-page-head>
      <h3 v-t="'lab_services.list'"> Services </h3>

      <template #right>
        <os-list-size
          :list="ctx.services"
          :page-size="ctx.pageSize"
          :list-size="ctx.servicesCount"
          @updateListSize="getServicesCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="showAddEditSvcDialog({})" />
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        class="os-list-card-items"
        :data="ctx.services"
        :schema="listSchema"
        :showRowActions="true"
        :expanded="ctx.expandedServices"
        :query="ctx.query"
        @filtersUpdated="loadServices"
        @rowClicked="onServiceRowClick"
        ref="listView">

        <template #rowActions="slotProps">
          <os-button-group>
            <os-button size="small" left-icon="edit" v-os-tooltip.bottom="$t('lab_services.edit_service')"
              @click="showAddEditSvcDialog(slotProps.rowObject)" v-show-if-allowed="'admin'" />
            <os-button size="small" left-icon="trash" v-os-tooltip.bottom="$t('lab_services.delete_service')"
              @click="deleteService(slotProps.rowObject)" v-show-if-allowed="'admin'" />
          </os-button-group>
        </template>

        <template #expansionRow="{rowObject}">
          <os-message type="info" v-if="rowObject.loadingRateLists">
            <span v-t="'lab_services.loading_rate_lists'">Loading rate lists...</span>
          </os-message>
          <os-message type="info" v-else-if="!rowObject.rateLists || rowObject.rateLists.length == 0">
            <span v-t="'lab_services.no_rate_lists'">No rates specified for this service...</span>
          </os-message>

          <pre>{{rowObject.rateLists}}</pre>
        </template>
      </os-list-view>
    </os-page-body>
  </os-page>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

import labSvc from '../services/LabService.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        services: [],

        servicesCount: -1,

        loading: true,

        query: this.filters,

        expandedServices: []
      },
 
      listSchema: labSvc.getListSchema()
    };
  },

  created() {
  },

  watch: {
  },

  computed: {
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadServices: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      this._reloadServices().then(() => routerSvc.goto('LabServicesList', {}, {filters: uriEncoding}));
    },

    getServicesCount: function() {
      const opts = Object.assign({}, this.ctx.filterValues || {});
      labSvc.getServicesCount(opts).then(({count}) => this.ctx.servicesCount = count);
    },

    _reloadServices: function() {
      this.ctx.loading = true;
      const opts = Object.assign({maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      return labSvc.getServices(opts || {}).then(
        resp => {
          this.ctx.loading = false;
          this.ctx.services = resp.map(service => ({service}));
          return this.ctx.services;
        }
      );
    }
  }
}
</script>
