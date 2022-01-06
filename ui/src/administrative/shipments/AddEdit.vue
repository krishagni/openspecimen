<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.shipment.id">Create Shipment</h3>
        <h3 v-else>Update #{{dataCtx.shipment.id}} {{dataCtx.shipment.name}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="ctx.loading">
        <os-message type="info">
          <span>Loading the form. Please wait for a moment...</span>
        </os-message>
      </div>
      <div v-else>
        <os-steps ref="shipmentWizard">
          <os-step title="Shipment Details" :validate="validateDetails">
            <os-form ref="shipmentDetails" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
              <div>
                <os-button primary label="Next" @click="next" />
                <os-button text label="Cancel" @click="cancel" />
              </div>
            </os-form>
          </os-step>

          <os-step title="Specimens" :validate="validateSpecimenDetails" v-if="dataCtx.shipment.type == 'SPECIMEN'">
            <span v-if="dataCtx.shipment.status == 'Pending'">
              <os-add-specimens ref="addSpmns" :criteria="ctx.criteria" :error-opts="ctx.errorOpts"
                label="Add specimens by scanning labels or barcodes separated by a comma, tab, or newline"
                @on-add="addSpecimens">
                <os-button label="Validate" @click="validateSpecimenLabels" />
              </os-add-specimens>
            </span>

            <div v-if="ctx.specimensSchema.columns.length > 0">
              <os-message type="error" v-if="!dataCtx.specimenItems || dataCtx.specimenItems.length == 0">
                <span>No specimens in the shipment. Add at least one specimen.</span>
              </os-message>

              <os-table-form ref="specimenDetails" v-else
                :data="dataCtx" :items="dataCtx.specimenItems" :schema="ctx.specimensSchema"
                :remove-items="dataCtx.shipment.status == 'Pending'" @remove-item="removeSpecimen($event)"
                @input="handleSpecimenItemInput($event)">
              </os-table-form>

              <os-divider />

              <div class="os-form-footer">
                <os-button secondary label="Previous" @click="previous" />
                <os-button primary label="Save Draft" @click="saveDraft" v-if="dataCtx.shipment.status == 'Pending'" />
                <os-button primary label="Ship" @click="ship" v-if="dataCtx.shipment.status == 'Pending'" />
                <os-button primary label="Receive" @click="receiveShipment" v-if="dataCtx.receive" />
                <os-button primary label="Update" @click="updateShipment" v-if="!dataCtx.receive && dataCtx.shipment.status != 'Pending'" />
                <os-button text label="Cancel" @click="cancel" />
              </div>

              <os-items-validation ref="validationsDialog" :report-messages="validationReportMsgs">
                <template #title>
                  <span>Specimens Validation Report</span>
                </template>
                <template #found>
                  <span>Passed</span>
                </template>
                <template #notFound>
                  <span>Failed: Specimens not present in the shipment</span>
                </template>
                <template #extras>
                  <span>Failed: Additional specimens present in the shipment</span>
                </template>
              </os-items-validation>
            </div>
          </os-step>

          <os-step title="Containers" :validate="validateContainerDetails" v-if="dataCtx.shipment.type == 'CONTAINER'">
            <span v-if="dataCtx.shipment.status == 'Pending'">
              <os-add-items ref="addContainers" @on-add="getAndAddContainers($event)"
                placeholder="Add containers by scanning names separated by a comma, tab, or newline">
              </os-add-items>
            </span>

            <div v-if="ctx.containersSchema.columns.length > 0">
              <os-message type="error" v-if="!dataCtx.containerItems || dataCtx.containerItems.length == 0">
                <span>No containers in the shipment. Add at least one container.</span>
              </os-message>

              <os-table-form ref="containerDetails" v-else
                :data="dataCtx" :items="dataCtx.containerItems" :schema="ctx.containersSchema"
                :remove-items="dataCtx.shipment.status == 'Pending'" @remove-item="removeContainer($event)"
                @input="handleContainerItemInput($event)">
              </os-table-form>

              <os-divider />

              <div class="os-form-footer">
                <os-button secondary label="Previous" @click="previous" />
                <os-button primary label="Save Draft" @click="saveDraft" v-if="dataCtx.shipment.status == 'Pending'" />
                <os-button primary label="Ship" @click="ship" v-if="dataCtx.shipment.status == 'Pending'" />
                <os-button primary label="Receive" @click="receiveShipment" v-if="dataCtx.receive" />
                <os-button primary label="Update" @click="updateShipment" v-if="!dataCtx.receive && dataCtx.shipment.status != 'Pending'" />
                <os-button text label="Cancel" @click="cancel" />
              </div>
            </div>
          </os-step>
        </os-steps>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject, watchEffect } from 'vue';

import alertSvc    from '@/common/services/Alerts.js';
import routerSvc   from '@/common/services/Router.js';
import util        from '@/common/services/Util.js';
import settingsSvc from '@/common/services/Setting.js';

import shipmentSvc from '@/administrative/services/Shipment.js';

import shipSpecimensSchema  from '@/administrative/schemas/shipments/ship-specimens.js';
import shipContainersSchema from '@/administrative/schemas/shipments/ship-containers.js';

export default {
  props: ['shipmentId', 'shipmentType', 'receive'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('ShipmentsList'), label: 'Shipments'}
      ],

      addEditFs: {rows: []},

      specimensSchema: {columns: []},

      containersSchema: {columns: []},

      loading: false
    });

    let dataCtx = reactive({
      shipment: {},

      currentUser: ui.currentUser,

      receive: props.receive,

      receivingInstitute: undefined,

      allowSpecimenRelabeling: false
    });


    settingsSvc.getSetting('administrative', 'allow_spmn_relabeling').then(
      setting => dataCtx.allowSpecimenRelabeling = util.isTrue(setting[0].value)
    );

    watchEffect(
      () => {
        ctx.loading = true;

        let promises = [ shipmentSvc.getAddEditFormSchema() ];
        if (props.shipmentId && +props.shipmentId > 0) {
          promises.push(shipmentSvc.getShipment(+props.shipmentId));

          if (props.shipmentType == 'SPECIMEN') {
            promises.push(shipmentSvc.getSpecimens(+props.shipmentId));
          } else {
            promises.push(shipmentSvc.getContainers(+props.shipmentId));
          }
        } else {
          const shipment = dataCtx.shipment = {
            type: props.shipmentType || 'SPECIMEN',
            shippedDate: new Date(),
            status: 'Pending',
            notifyUsers: []
          };

          if (shipment.type == 'SPECIMEN') { // TODO: remove when everything is Vue based
            const spmnsQ  = shipmentSvc.getSelectedSpecimens(localStorage.selectedSpecimenIds);
            if (spmnsQ) {
              promises.push(spmnsQ);
            }

            localStorage.removeItem('selectedSpecimenIds');
          }

          dataCtx.specimenItems = [];
          dataCtx.containerItems = [];
        }

        Promise.all(promises).then(
          function(result) {
            ctx.loading   = false;
            ctx.addEditFs = result[0].schema;
            if (result.length > 1) {
              let idx = 1;
              if (result.length == 3) {          // when there are 3 promises/results
                dataCtx.shipment = result[idx];  // the second argument (idx = 1) is shipment
                dataCtx.receivingInstitute = result[idx].receivingInstitute;
                ++idx;
              }

              if (dataCtx.shipment.type == 'SPECIMEN') {
                dataCtx.specimenItems  = result[idx];
              } else {
                dataCtx.containerItems = result[idx];
              }
            }

            if (props.receive) {
              dataCtx.shipment.receivedDate = new Date();

              if (dataCtx.shipment.type == 'SPECIMEN') {
                dataCtx.specimenItems.forEach(item => item.specimen.storageLocation = null);
              } else {
                dataCtx.containerItems.forEach(item => item.container.storageLocation = null);
              }
            }

            ctx.specimensSchema  =  shipSpecimensSchema;
            ctx.containersSchema =  shipContainersSchema;
          },

          function() {
            ctx.loading = false;
          }
        );
      }
    );

    return { ctx, dataCtx };
  },

  computed: {
    validationReportMsgs: function() {
      return {
        label: this.$refs.addSpmns && this.$refs.addSpmns.useBarcode ? 'Barcode' : 'Label',
        error: 'Error',
        notFound: 'Not present in the shipment',
        extra: 'Additional specimen present in the shipment'
      }
    }
  },

  methods: {
    handleInput: function({field, value, data}) {
      Object.assign(this.dataCtx, data);
      if (field.name == 'shipment.receivingInstitute' && this.dataCtx.receivingInstitute != value) {
        this.dataCtx.receivingInstitute = value;
        this.dataCtx.shipment.receivingSite = undefined;
        this.dataCtx.shipment.notifyUsers = [];
      }
    },

    next: function() {
      this.$refs.shipmentWizard.next();
    },

    previous: function() {
      this.$refs.shipmentWizard.previous();
    },

    validateDetails: function() {
      return this.$refs.shipmentDetails.validate();
    },

    validateSpecimenDetails: function() {
      if (!this.dataCtx.specimenItems  || this.dataCtx.specimenItems.length == 0) {
        return true;
      }

      return this.$refs.specimenDetails.validate();
    },

    validateSpecimenLabels: function() {
      const labels = this.$refs.addSpmns.getLabels();
      const prop   = this.$refs.addSpmns.useBarcode ? 'specimen.barcode' : 'specimen.label';
      this.$refs.validationsDialog.validate(this.dataCtx.specimenItems, labels, prop);
    },

    handleSpecimenItemInput: function({item, itemIdx}) {
      Object.assign(this.dataCtx.specimenItems[itemIdx], item);
    },

    addSpecimens: function({specimens}) {
      let toAdd = specimens.map(specimen => ({specimen: specimen}));
      util.addIfAbsent(this.dataCtx.specimenItems, toAdd, 'specimen.id');
    },

    removeSpecimen: function({idx}) {
      this.dataCtx.specimenItems.splice(idx, 1);
    },

    validateContainerDetails: function() {
      if (!this.dataCtx.containerItems  || this.dataCtx.containerItems.length == 0) {
        return true;
      }

      return this.$refs.containerDetails.validate();
    },

    handleContainerItemInput: function({item, itemIdx}) {
      Object.assign(this.dataCtx.containerItems[itemIdx], item);
    },

    getAndAddContainers: async function({itemLabels}) {
      const shipment   = this.dataCtx.shipment;
      const containers = await shipmentSvc.searchContainers(shipment.sendingSite, shipment.receivingSite, itemLabels);
      const toAdd      = containers.map(container => ({container: container}));
      util.addIfAbsent(this.dataCtx.containerItems, toAdd, 'container.id');
    },

    removeContainer: function({idx}) {
      this.dataCtx.containerItems.splice(idx, 1);
    },

    saveDraft: function() {
      this.saveOrUpdate('Pending').then(shipment => alertSvc.success('Draft shipment "' + shipment.name + '" saved!'));
    },

    ship: function() {
      this.saveOrUpdate('Shipped').then(shipment => alertSvc.success('Shipment "' + shipment.name + '" shipped!'));
    },

    receiveShipment: function() {
      this.saveOrUpdate('Received').then(shipment => alertSvc.success('Shipment "' + shipment.name + '" received!'));
    },

    updateShipment: function() {
      this.saveOrUpdate(null).then(shipment => alertSvc.success('Shipment "' + shipment.name + '" updated!'));
    },

    saveOrUpdate: async function(status) {
      if (this.dataCtx.shipment.type == 'SPECIMEN') {
        if (!this.dataCtx.specimenItems || this.dataCtx.specimenItems.length == 0) {
          alertSvc.error('No specimens in the shipment. Add at least one specimen.');
          return;
        }

        if (!this.$refs.specimenDetails.validate()) {
          return;
        }
      } else {
        if (!this.dataCtx.containerItems || this.dataCtx.containerItems.length == 0) {
          alertSvc.error('No containers in the shipment. Add at least one container.');
          return;
        }

        if (!this.$refs.containerDetails.validate()) {
          return;
        }
      }

      const toSave = JSON.parse(JSON.stringify(this.dataCtx.shipment));
      if (toSave.type == 'SPECIMEN') {
        toSave.shipmentSpmns = JSON.parse(JSON.stringify(this.dataCtx.specimenItems));
      } else {
        toSave.shipmentContainers = JSON.parse(JSON.stringify(this.dataCtx.containerItems));
      }

      if (status) {
        toSave.status = status;
      }

      const savedShipment = await shipmentSvc.saveOrUpdate(toSave);
      routerSvc.goto('ShipmentOverview', {shipmentId: savedShipment.id});
      return savedShipment;
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
