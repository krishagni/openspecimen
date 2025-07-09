<template>
  <os-panel>
    <template #header>
      <span class="title" v-t="'cps.services'">Services</span>
    </template>

    <template #primary-actions>
      <os-button left-icon="plus" :label="$t('common.buttons.add')"
        v-os-tooltip.bottom="$t('cps.add_service')" @click="showAddEditSvcDialog({})"
        v-show-if-allowed="cpResources.updateOpts" />
    </template>

    <div>
      <os-list-view
        class="os-cp-services"
        :data="services"
        :schema="servicesListSchema"
        :showRowActions="true"
        :expanded="expandedServices"
        @rowClicked="onServiceRowClick"
        ref="listView">

        <template #rowActions="slotProps">
          <os-button-group>
            <os-button size="small" left-icon="edit" v-os-tooltip.bottom="$t('cps.edit_service')"
              @click="showAddEditSvcDialog(slotProps.rowObject)" v-show-if-allowed="cpResources.updateOpts" />
            <os-button size="small" left-icon="trash" v-os-tooltip.bottom="$t('cps.delete_service')"
              @click="deleteService(slotProps.rowObject)" v-show-if-allowed="cpResources.updateOpts" />
            <os-button size="small" left-icon="plus" v-os-tooltip.bottom="$t('cps.add_service_rate')"
              @click="showAddEditRateDialog(slotProps.rowObject, {})" v-show-if-allowed="cpResources.updateOpts" />
          </os-button-group>
        </template>

        <template #expansionRow="{rowObject}">
          <os-message type="info" v-if="rowObject.loadingRates">
            <span v-t="'cps.loading_rates'">Loading rates...</span>
          </os-message>
          <os-message type="info" v-else-if="!rowObject.rates || rowObject.rates.length == 0">
            <span v-t="'cps.no_service_rates'">No rates specified for this service...</span>
          </os-message>
          <os-list-view
            class="service-rates"
            :data="rowObject.rates"
            :schema="ratesListSchema"
            :showRowActions="true" v-else>
            <template #rowActions="slotProps">
              <os-button-group>
                <os-button size="small" left-icon="edit" v-os-tooltip.bottom="$t('cps.edit_service_rate')"
                  @click="showAddEditRateDialog(rowObject, slotProps.rowObject)"
                  v-show-if-allowed="cpResources.updateOpts" />
                <os-button size="small" left-icon="trash" v-os-tooltip.bottom="$t('cps.delete_service_rate')"
                  @click="deleteRate(rowObject, slotProps.rowObject)" v-show-if-allowed="cpResources.updateOpts" />
              </os-button-group>
            </template>
          </os-list-view>
        </template>
      </os-list-view>
    </div>

    <os-dialog ref="addEditSvcDialog">
      <template #header>
        <span v-if="addEditSvcCtx.service.id > 0" v-t="'cps.edit_service'">Edit Service</span>
        <span v-else v-t="'cps.add_service'">Add Service</span>
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
        <span v-t="{path: 'cps.confirm_delete_service', args: deleteSvcCtx}">Are you sure you want to delete the service {0}?</span>
      </template>
    </os-confirm-delete>

    <os-dialog ref="addEditRateDialog">
      <template #header>
        <span v-if="addEditRateCtx.rate.id > 0" v-t="'cps.edit_service_rate'">Edit Rate</span>
        <span v-else v-t="'cps.add_service_rate'">Add Rate</span>
      </template>

      <template #content>
        <os-form ref="addEditRateForm" :schema="addEditRateSchema" :data="addEditRateCtx" />
      </template>

      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="hideAddEditRateDialog" />
        <os-button primary :label="$t(addEditRateCtx.rate.id > 0 ? 'common.buttons.update' : 'common.buttons.add')"
          @click="addEditRate" />
      </template>
    </os-dialog>

    <os-confirm-delete ref="confirmDeleteRateDialog" :captcha="false" :collect-reason="false">
      <template #message>
        <span v-t="{path: 'cps.confirm_delete_service_rate', args: deleteRateCtx}">Are you sure you want to delete the service {0} rate for the interval {1}</span>
      </template>
    </os-confirm-delete>
  </os-panel>
</template>

<script>
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

import addEditSvcSchema   from '@/biospecimen/schemas/cps/addedit-service.js';
import addEditRateSchema  from '@/biospecimen/schemas/cps/addedit-rate.js';
import ratesListSchema    from '@/biospecimen/schemas/cps/rates-list.js';
import servicesListSchema from '@/biospecimen/schemas/cps/services-list.js';

import cpResources from './Resources.js';

export default {
  props: ['cp', 'serviceId'],

  data() {
    return {
      service: null,

      services: [],

      expandedServices: [],

      addEditSvcCtx: {},

      deleteSvcCtx: {},

      addEditRateCtx: {},

      deleteRateCtx: {},

      addEditSvcSchema: addEditSvcSchema.layout,

      addEditRateSchema: addEditRateSchema.layout,

      ratesListSchema,

      servicesListSchema,

      cpResources
    }
  },

  mounted() {
    this._loadServices().then(() => this._expandServiceCard());
  },

  watch: {
    serviceId: function() {
      this._expandServiceCard();
    }
  },

  methods: {
    onServiceRowClick: function(rowObject) {
      let serviceId = null;
      if (rowObject.service.id != +this.serviceId) {
        serviceId = rowObject.service.id;
      }

      routerSvc.goto('CpDetail.Settings.Services', {cpId: this.cp.id}, serviceId && {serviceId});
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
      service.cpId = this.cp.id;
      cpSvc.addEditService(service).then(
        saved => {
          if (service.id > 0) {
            this._refreshService(saved.id);
          } else {
            this._loadServices().then(
              () => {
                routerSvc.goto('CpDetail.Settings.Services', {cpId: this.cp.id}, {serviceId: saved.id});
              }
            );
          }

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

          cpSvc.deleteService(service.id).then(
            () => {
              this._loadServices();
              routerSvc.goto('CpDetail.Settings.Services', {cpId: this.cp.id});
            }
          );
        }
      );
    },

    showAddEditRateDialog: function({service}, {rate}) {
      rate = util.clone(rate || {});

      this.addEditRateCtx = {service, rate};
      this.$refs.addEditRateDialog.open();
    },

    hideAddEditRateDialog: function() {
      this.$refs.addEditRateDialog.close();
    },

    addEditRate: function() {
      if (!this.$refs.addEditRateForm.validate()) {
        return;
      }

      const {service, rate} = this.addEditRateCtx;
      cpSvc.addEditServiceRate(service.id, rate).then(
        () => {
           this._refreshService(service.id);
           this.hideAddEditRateDialog();
        }
      );
    },

    deleteRate: function({service}, {rate}) {
      const startDate = util.formatDate(rate.startDate);
      const endDate = rate.endDate ? util.formatDate(rate.endDate) : i18n.msg('cps.eol');
      this.deleteRateCtx = {
        service: service.code,
        interval: startDate + ' - ' + endDate
      };

      this.$refs.confirmDeleteRateDialog.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          cpSvc.deleteServiceRate(service.id, rate.id).then(() => this._refreshService(service.id));
        }
      );
    },

    _expandServiceCard: function() {
      const [prev] = this.expandedServices;
      this.expandedServices.length = 0;

      if (+this.serviceId > 0) {
        const serviceCard = this.services.find(({service}) => service.id == +this.serviceId);
        if (serviceCard) {
          if (!prev || prev.service != serviceCard.service) {
            this.expandedServices.push(serviceCard);
            this._loadServiceRates(serviceCard);
          }
        }
      }
    },

    _loadServices: function() {
      const rateEffectiveOn = this._getCurrentDate();
      return cpSvc.getServices(this.cp.id, {rateEffectiveOn}).then(
        services => {
          this.services = services.map(service => ({service}));
          return this.services;
        }
      );
    },

    _refreshService: function(serviceId) {
      const serviceCard = this.services.find(card => card.service.id == serviceId);
      const filterOpts = {query: serviceCard.service.code, rateEffectiveOn: this._getCurrentDate()};

      cpSvc.getServices(this.cp.id, filterOpts).then(
        services => {
          const match = services.find(s => s.id == serviceCard.service.id);
          serviceCard.service = match;
        }
      );

      this._loadServiceRates(serviceCard, true);
    },

    _loadServiceRates: function(rowObject, reload) {
      if (!reload && rowObject.rates) {
        return;
      }

      rowObject.loadingRates = true;
      cpSvc.getServiceRates(rowObject.service.id).then(
        rates => {
          rowObject.rates = rates.map(rate => ({rate}));
          rowObject.loadingRates = false;
        }
      );
    },

    _getCurrentDate: function() {
      const dt = new Date();
      const year = dt.getFullYear();
      const month = (dt.getMonth() + 1).toString().padStart(2, '0');
      const date = dt.getDate().toString().padStart(2, '0');
      return year + '-' + month + '-' + date;
    }
  }
}
</script>

<style scoped>
.os-cp-services :deep(> .results > .results-inner > .p-datatable > .p-datatable-wrapper > table.p-datatable-table) {
  border: 0px;
  border-collapse: separate;
  border-spacing: 2px 15px;
  margin-top: -0.5rem;
  margin-bottom: 0rem;
  table-layout: fixed;
}

.os-cp-services :deep(> .results > .results-inner > .p-datatable > .p-datatable-wrapper > table.p-datatable-table > tbody > tr) {
  box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .3), 0 2px 6px 2px rgba(60, 64, 67, .15);
  border-radius: 0.5rem;
  background: transparent;
}

.os-cp-services :deep(> .results > .results-inner > .p-datatable > .p-datatable-wrapper > table.p-datatable-table > tbody > tr > td) {
  padding: 1rem;
  vertical-align: middle;
  border-top: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
