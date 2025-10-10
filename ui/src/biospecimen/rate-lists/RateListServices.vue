<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="showAddEditServicesDialog"
          v-if="!ctx.selectedServices || ctx.selectedServices.length == 0" />
        <os-button left-icon="times" :label="$t('common.buttons.remove')" @click="removeServices" v-else />
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
        <span v-t="'lab_services.add_services'">Add Services</span>
      </template>
      <template #content>
        <os-table-form ref="svcsForm" :schema="addEditSvcsSchema" :data="ctx" :items="ctx.serviceRates"
          :remove-items="true" @remove-item="removeServiceRateItem($event)">
          <template #default>
            <os-button :label="$t('common.buttons.add_another')" @click="addServiceRateItem" />
          </template>
        </os-table-form>
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="hideAddEditServicesDialog" />

        <os-button primary :label="$t('common.buttons.add')" @click="addServices" />
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

      addEditSvcsSchema: {
        columns: [
          {
            type: 'dropdown',
            labelCode: 'lab_services.service',
            name: 'serviceRate.service',
            listSource: {
              apiUrl: 'lab-services',
              searchProp: 'query',
              displayProp: (option) => option.description + ' (' + option.code + ')',
              queryParams: {
                dynamic: {
                  notInRateListId: 'rateList.id'
                }
              }
            },
            validations: {
              required: {
                messageCode: 'lab_services.service_req'
              }
            },
            uiStyle: {
              'width': '550px'
            }
          },
          {
            type: 'number',
            maxFractionDigits: 2,
            labelCode: 'lab_services.rate',
            name: 'serviceRate.rate',
            validations: {
              required: {
                messageCode: 'lab_services.rate_req'
              }
            }
          }
        ]
      }
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

    showAddEditServicesDialog: function() {
      this.ctx.serviceRates = [{}],
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

    addServices: function() {
      if (!this.$refs.svcsForm.validate()) {
        return;
      }

      const servicesToAdd = this.ctx.serviceRates.map(
        ({serviceRate}) => ({serviceCode: serviceRate.service.code, rate: serviceRate.rate})
      );
      return rateListSvc.upsertServices(this.rateList.id, servicesToAdd).then(
        ({count}) => {
          alertsSvc.success({code: 'lab_services.services_added', args: {count}});
          this.$refs.listView.reload();
          this.$emit('rate-list-services-added', {count});
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
