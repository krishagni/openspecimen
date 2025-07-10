<template>
  <os-page-toolbar>
    <os-button left-icon="plus" :label="$t('cps.add_service')" @click="showAddEditServiceDialog({})"
      v-if="isUpdateAllowed" />
  </os-page-toolbar>

  <os-grid>
    <os-grid-column :width="12">
      <os-list-view
        :data="services"
        :schema="servicesListSchema"
        :showRowActions="true">

        <template #rowActions="slotProps">
          <div style="width: 5rem;">
            <os-button-group v-if="isUpdateAllowed">
              <os-button size="small" left-icon="edit" @click="showAddEditServiceDialog(slotProps.rowObject)"
                v-os-tooltip.bottom="$t('cps.edit_service')" />
              <os-button size="small" left-icon="trash" @click="deleteService(slotProps.rowObject)"
                v-os-tooltip.bottom="$t('cps.delete_service')" />
            </os-button-group>
          </div>
        </template>
        <template #footerRow>
          <span v-t="{path: 'specimens.total_service_rate', args: {totalRate}}">Total Rate: {{totalRate}}</span>
        </template>
      </os-list-view>
    </os-grid-column>

    <os-dialog ref="addEditServiceDialog">
      <template #header>
        <span v-t="'cps.add_service'" v-if="!addEditCtx.serviceDetail.id">Add Service</span>
        <span v-t="'cps.edit_service'" v-else>Edit Service</span>
      </template>
      <template #content>
        <os-form ref="addEditServiceForm" :data="addEditCtx" :schema="addEditServiceSchema" />
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="hideAddEditServiceDialog" />
        <os-button primary :label="$t(addEditCtx.serviceDetail.id > 0 ? 'common.buttons.update' : 'common.buttons.add')"
          @click="addOrEditService" />
      </template>
    </os-dialog>

    <os-confirm-delete ref="deleteServiceDialog" :captcha="false">
      <template #message>
        <span v-t="{path: 'specimens.confirm_delete_service', args: deleteCtx}">Are you sure you want to delete {0} service done on {1}?</span>
      </template>
    </os-confirm-delete>
  </os-grid>
</template>

<script>
import specimenSvc from '@/biospecimen/services/Specimen.js';
import util        from '@/common/services/Util.js';

import addEditServiceSchema from '@/biospecimen/schemas/specimens/addedit-service.js';
import servicesListSchema   from '@/biospecimen/schemas/specimens/services-list.js';

export default {
  props: ['cpr'],

  inject: ['cpViewCtx', 'specimen'],

  data() {
    return {
      services: [],

      totalRate: 0,

      addEditCtx: {
        serviceDetail: {}
      },

      deleteCtx: {
        serviceDetail: {}
      },

      addEditServiceSchema: addEditServiceSchema.layout,

      servicesListSchema
    }
  },

  mounted() {
    this._loadServices();
  },

  computed: {
    notCoordinatOrStoreAllowed: function() {
      return this.cpViewCtx.notCoordinatOrStoreAllowed(this.specimen || {});
    },

    isAnyUserUpdateAllowed: function() {
      const vc = this.cpViewCtx;
      const {lineage} = this.specimen;
      return lineage == 'New' ? vc.isUpdateSpecimenAllowed(this.cpr) : vc.isUpdateAllSpecimenAllowed(this.cpr);
    },

    isUpdateAllowed: function() {
      return this.isAnyUserUpdateAllowed && this.notCoordinatOrStoreAllowed;
    }
  },

  methods: {
    showAddEditServiceDialog: function({service}) {
      if (service && service.id > 0) {
        const detail = this.addEditCtx.serviceDetail = util.clone(service);
        detail.cpId = this.specimen.cpId;
      } else {
        this.addEditCtx.serviceDetail = {
          cpId: this.specimen.cpId,
          specimenId: this.specimen.id,
          servicedBy: this.$ui.currentUser,
          serviceDate: Date.now()   
        };
      }

      this.$refs.addEditServiceDialog.open();
    },

    hideAddEditServiceDialog: function() {
      this.addEditCtx.serviceDetail = {};
      this.$refs.addEditServiceDialog.close();
    },

    addOrEditService: function() {
      if (!this.$refs.addEditServiceForm.validate()) {
        return;
      }

      const {serviceDetail} = this.addEditCtx;
      specimenSvc.addOrUpdateLabService(serviceDetail).then(
        () => {
          this._loadServices();
          this.hideAddEditServiceDialog();
        }
      );
    },

    deleteService: function({service}) {
      const {global: {locale: {dateFmt}}} = this.$ui;
      const serviceDate = util.formatDate(new Date(service.serviceDate), dateFmt);
      this.deleteCtx = {serviceCode: service.serviceCode, serviceDate};
      this.$refs.deleteServiceDialog.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          specimenSvc.deleteLabService(service).then(() => this._loadServices())
        }
      );
    },

    _loadServices: function() {
      specimenSvc.getLabServices(this.specimen.id, {includeRates: true}).then(
        services => {
          this.services = services.map(service => ({service}));
          this.totalRate = services.reduce((acc, svc) => svc.serviceRate >= 0 ? acc + svc.serviceRate : acc, 0);
          this.totalRate = this.totalRate.toFixed(2);
        }
      );
    }
  }
}
</script>
