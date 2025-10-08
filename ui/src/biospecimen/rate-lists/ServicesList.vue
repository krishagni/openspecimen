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
            <span v-t="'lab_services.service_not_used_in_rl'">No rates specified for this service...</span>
          </os-message>
          <os-list-view :data="rowObject.rateLists" :schema="serviceRatesListSchema" v-else />
        </template>
      </os-list-view>
    </os-page-body>

    <os-dialog ref="addEditSvcDialog">
      <template #header>
        <span v-if="addEditSvcCtx.service.id > 0" v-t="'lab_services.edit_service'">Edit Service</span>
        <span v-else v-t="'lab_services.add_service'">Add Service</span>
      </template>

      <template #content>
        <os-form ref="addEditSvcForm" :schema="addEditSvcSchema" :data="addEditSvcCtx" />
      </template>

      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="hideAddEditSvcDialog" />
        <os-button primary :label="$t(addEditSvcCtx.service.id > 0 ? 'common.buttons.update' : 'common.buttons.add')"
          @click="addEditService" />
      </template>
    </os-dialog>

    <os-confirm-delete ref="confirmDeleteSvcDialog" :captcha="false" :collect-reason="false">
      <template #message>
        <span v-t="{path: 'lab_services.confirm_delete_service', args: deleteSvcCtx}">Are you sure you want to delete the service {0}?</span>
      </template>
    </os-confirm-delete>
  </os-page>
</template>

<script>
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

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

      addEditSvcCtx: {},

      addEditSvcSchema: labSvc.getAddEditSchema(),
 
      listSchema: labSvc.getListSchema(),

      serviceRatesListSchema: labSvc.getServiceRatesListSchema()
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

    showAddEditSvcDialog: function({service}) {
      this.addEditSvcCtx = {service: util.clone(service || {})};
      this.$refs.addEditSvcDialog.open();
    },

    hideAddEditSvcDialog: function() {
      this.$refs.addEditSvcDialog.close();
    },

    addEditService: function() {
      if (!this.$refs.addEditSvcForm.validate()) {
        return;
      }

      const {service} = this.addEditSvcCtx;
      labSvc.saveOrUpdate(service).then(
        saved => {
          this._refreshService(saved);
          this.hideAddEditSvcDialog();
        }
      );
    },

    deleteService: function({service}) {
      this.deleteSvcCtx = { service: service.code };
      this.$refs.confirmDeleteSvcDialog.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          labSvc.deleteService(service.id).then(() => this._reloadServices());
        }
      );
    },

    onServiceRowClick: function(rowObject) {
      if (this.ctx.expandedServices.indexOf(rowObject) >= 0) {
        this.ctx.expandedServices = [];
        return;
      }

      this.ctx.expandedServices = [rowObject];
      if (!rowObject.rateLists) {
        rowObject.loadingRateLists = true;
        labSvc.getRateLists(rowObject.service.id).then(
          rateLists => {
            rowObject.rateLists = rateLists.map(rateList => ({rateList}));
            rowObject.loadingRateLists = false;
          }
        );
      }
    },

    _reloadServices: function() {
      this.ctx.loading = true;
      const opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      return labSvc.getServices(opts || {}).then(
        resp => {
          this.ctx.loading = false;
          this.ctx.services = resp.map(service => ({service}));
          return this.ctx.services;
        }
      );
    },

    _refreshService: function(saved) {
      const rowObject = this.ctx.services.find(({service}) => service.id == saved.id);
      if (rowObject) {
        Object.assign(rowObject.service, saved);
      } else {
        this.ctx.services.unshift({service: saved});
      }
    }
  }
}
</script>
