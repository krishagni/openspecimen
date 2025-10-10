<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="showAddEditServicesDialog" />
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

    <os-dialog ref="addEditServicesDialog">
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
              'width': '450px'
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
    }
  }
}
</script>
