<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="showAddEditServicesDialog"
          v-if="!ctx.selectedServices || ctx.selectedServices.length == 0" />
        <span v-else>
          <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editServices" />
          <os-button left-icon="times" :label="$t('common.buttons.remove')" @click="removeServices" />
        </span>
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
        :allowSelection="true"
        @filtersUpdated="loadServices"
        @rowClicked="onServiceRowClick"
        @selectedRows="onServicesSelection"
        ref="listView"
      />
    </os-grid-column>

    <os-dialog ref="addEditServicesDialog" size="lg">
      <template #header>
        <span v-t="'lab_services.edit_services'" v-if="ctx.editServices">Edit Services</span>
        <span v-t="'lab_services.add_services'" v-else>Add Services</span>
      </template>
      <template #content>
        <os-table-form ref="svcsForm" :schema="addEditSvcsSchema" :data="ctx" :items="ctx.serviceRates"
          :remove-items="!ctx.editServices" @remove-item="removeServiceRateItem($event)">
          <template #default v-if="!ctx.editServices">
            <os-button :label="$t('common.buttons.add_another')" @click="addServiceRateItem" />
          </template>
        </os-table-form>
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="hideAddEditServicesDialog" />

        <os-button primary :label="$t(ctx.editServices ? 'common.buttons.update' : 'common.buttons.add')"
          @click="saveOrUpdateServices" />
      </template>
    </os-dialog>

    <os-confirm ref="removeSvcsConfirmDialog">
      <template #title>
        <span v-t="'lab_services.remove_services'">Remove Services</span>
      </template>
      <template #message>
        <span v-t="'lab_services.remove_services_confirm'">Are you sure you want to remove selected services from the rate list?</span>
      </template>
    </os-confirm>
  </os-grid>
</template>

<script>
import alertsSvc   from '@/common/services/Alerts.js';
import rateListSvc from '@/biospecimen/services/RateList.js';
import routerSvc   from '@/common/services/Router.js';
import util        from '@/common/services/Util.js';

export default {
  props: ['rate-list'],

  data() {
    return {
      ctx: {
        services: [],

        loading: false,

        selectedServices: [],

        serviceRates: []
      },

      cpUpdateOpts: {resource: 'CollectionProtocol', operations: ['Create', 'Update']},

      listSchema: rateListSvc.getServicesListSchema(),

      addEditSvcsSchema: rateListSvc.getAddEditServicesSchema()
    }
  },

  methods: {
    loadServices: function() {
      this.ctx.loading = true;
      this.ctx.rateList = this.rateList;
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
    },

    onServicesSelection: function(selection) {
      this.ctx.selectedServices = (selection || []).map(({rowObject: {service}}) => service);
    },

    editServices: function() {
      this.ctx.serviceRates = util.clone(this.ctx.selectedServices);
      for (const rate of this.ctx.serviceRates) {
        rate.service = {code: rate.serviceCode, description: rate.serviceDescription + ' (' + rate.serviceCode + ')'};
      }

      this.ctx.serviceRates = this.ctx.serviceRates.map(serviceRate => ({serviceRate}));
      this.ctx.editServices = true;
      this.$refs.addEditServicesDialog.open();
    },

    showAddEditServicesDialog: function() {
      this.ctx.serviceRates = [{}],
      this.ctx.editServices = false;
      this.$refs.addEditServicesDialog.open();
    },

    hideAddEditServicesDialog: function() {
      this.$refs.addEditServicesDialog.close();
    },

    addServiceRateItem: function() {
      this.ctx.serviceRates.push({});
    },

    removeServiceRateItem: function({idx}) {
      this.ctx.serviceRates.splice(idx, 1);
      if (this.ctx.serviceRates.length == 0) {
        this.addServiceRateItem();
      }
    },

    saveOrUpdateServices: function() {
      if (!this.$refs.svcsForm.validate()) {
        return;
      }

      const toSave = this.ctx.serviceRates.map(({serviceRate: {service, rate}}) => ({serviceCode: service.code, rate}));
      return rateListSvc.upsertServices(this.rateList.id, toSave).then(
        ({count}) => {
          if (this.ctx.editServices) {
            alertsSvc.success({code: 'lab_services.services_updated', args: {count}});
          } else {
            alertsSvc.success({code: 'lab_services.services_added', args: {count}});
            this.$emit('rate-list-services-added', {count});
          }

          this.$refs.listView.reload();
          this.hideAddEditServicesDialog();
        }
      );
    },

    removeServices: async function() {
      const resp = await this.$refs.removeSvcsConfirmDialog.open();
      if (resp != 'proceed') {
        return;
      }

      const {selectedServices} = this.ctx;
      rateListSvc.removeServices(this.rateList.id, selectedServices).then(
        ({count}) => {
          alertsSvc.success({code: 'lab_services.services_removed', args: {count}});
          this.$refs.listView.reload();
          this.$emit('rate-list-services-removed', {count});
        }
      );
    }
  }
}
</script>
