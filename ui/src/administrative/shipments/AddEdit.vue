<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.shipment.id">
          <span v-t="'shipments.create'">Create Shipment</span>
        </h3>
        <h3 v-else>
          <span v-t="{path: 'shipments.update', args: dataCtx.shipment}">Update #{{dataCtx.shipment.id}} {{dataCtx.shipment.name}}</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="ctx.loading">
        <os-message type="info">
          <span v-t="'common.loading_form'">Loading the form. Please wait for a moment...</span>
        </os-message>
      </div>
      <div v-else>
        <os-steps ref="shipmentWizard">
          <os-step :title="$t('shipments.details')" :validate="validateDetails">
            <os-form ref="shipmentDetails" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
              <div>
                <os-button primary :label="$t('common.buttons.next')" @click="next" />
                <os-button text    :label="$t('common.buttons.cancel')" @click="cancel" />
              </div>
            </os-form>
          </os-step>

          <os-step :title="$t('shipments.specimens')" :validate="validateSpecimenDetails"
            v-if="dataCtx.shipment.type == 'SPECIMEN'">

            <span v-if="dataCtx.shipment.status == 'Pending' && dataCtx.specimenItems.length < ctx.maxSpmnsLimit">
              <os-add-specimens ref="addSpmns" :criteria="ctx.criteria" :error-opts="ctx.errorOpts"
                :label="$t('shipments.scan_specimen_labels')" @on-add="addSpecimens">
                <os-button :label="$t('shipments.validate')" @click="validateSpecimenLabels" />
              </os-add-specimens>
            </span>

            <div v-if="ctx.specimensSchema.columns.length > 0">
              <os-message type="error" v-if="!dataCtx.specimenItems || dataCtx.specimenItems.length == 0">
                <span v-t="'shipments.no_specimens_add_one'">No specimens in the shipment. Add at least one specimen.</span>
              </os-message>

              <div v-else-if="dataCtx.specimenItems.length > ctx.maxSpmnsLimit">
                <os-message type="warn">
                  <span v-t="{path: 'shipments.max_spmns_limit_exceeded', args: ctx}">More specimens in the shipment than allowed to edit on the UI. Use Bulk Import</span>
                </os-message>

                <os-button v-if="dataCtx.shipment.id > 0" left-icon="download" :label="$t('shipments.export_shipment')"
                  @click="exportShipment" />
              </div>

              <os-table-form ref="specimenDetails" v-else
                :data="dataCtx" :items="dataCtx.specimenItems" :schema="ctx.specimensSchema"
                :remove-items="dataCtx.shipment.status == 'Pending'" @remove-item="removeSpecimen($event)"
                @input="handleSpecimenItemInput($event)">
              </os-table-form>

              <os-divider />

              <div class="os-form-footer">
                <os-button secondary :label="$t('common.buttons.previous')" @click="previous" />
                <os-button primary :label="$t('shipments.save_draft')" @click="saveDraft"
                  v-if="dataCtx.shipment.status == 'Pending'" />
                <os-button primary :label="$t('shipments.ship')" @click="ship"
                  v-if="dataCtx.ship || (!dataCtx.shipment.request && !dataCtx.shipment.id)" />
                <os-button primary :label="$t('shipments.request')" @click="request"
                  v-if="dataCtx.shipment.request && dataCtx.shipment.status == 'Pending'" />
                <os-button primary :label="$t('shipments.receive')" @click="receiveShipment"
                  v-if="dataCtx.receive" />
                <os-button primary :label="$t('common.buttons.update')" @click="updateShipment"
                  v-if="!dataCtx.ship && !dataCtx.receive && dataCtx.shipment.status != 'Pending'" />
                <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
              </div>

              <os-items-validation ref="validationsDialog" :report-messages="validationReportMsgs">
                <template #title>
                  <span v-t="'shipments.specimens_validation_report'">Specimens Validation Report</span>
                </template>
                <template #found>
                  <span v-t="'shipments.passed'">Passed</span>
                </template>
                <template #notFound>
                  <span v-t="'shipments.specimens_not_present'">Failed: Specimens not present in the shipment</span>
                </template>
                <template #extras>
                  <span v-t="'shipments.extra_specimens'">Failed: Additional specimens present in the shipment</span>
                </template>
              </os-items-validation>
            </div>
          </os-step>

          <os-step :title="$t('shipments.containers')" :validate="validateContainerDetails"
            v-if="dataCtx.shipment.type == 'CONTAINER'">
            <span v-if="dataCtx.shipment.status == 'Pending'">
              <os-add-items ref="addContainers" @on-add="getAndAddContainers($event)"
                :placeholder="$t('shipments.scan_container_names')">
              </os-add-items>
            </span>

            <div v-if="ctx.containersSchema.columns.length > 0">
              <os-message type="error" v-if="!dataCtx.containerItems || dataCtx.containerItems.length == 0">
                <span v-t="'shipments.no_containers_add_one'">No containers in the shipment. Add at least one container.</span>
              </os-message>

              <os-table-form ref="containerDetails" v-else
                :data="dataCtx" :items="dataCtx.containerItems" :schema="ctx.containersSchema"
                :remove-items="dataCtx.shipment.status == 'Pending'" @remove-item="removeContainer($event)"
                @input="handleContainerItemInput($event)">
              </os-table-form>

              <os-divider />

              <div class="os-form-footer">
                <os-button secondary :label="$t('common.buttons.previous')" @click="previous" />
                <os-button primary :label="$t('shipments.save_draft')" @click="saveDraft"
                  v-if="dataCtx.shipment.status == 'Pending'" />
                <os-button primary :label="$t('shipments.ship')" @click="ship"
                  v-if="dataCtx.ship || (!dataCtx.shipment.request && !dataCtx.shipment.id)" />
                <os-button primary :label="$t('shipments.request')" @click="request"
                  v-if="dataCtx.shipment.request && dataCtx.shipment.status == 'Pending'" />
                <os-button primary :label="$t('shipments.receive')" @click="receiveShipment"
                  v-if="dataCtx.receive" />
                <os-button primary :label="$t('common.buttons.update')" @click="updateShipment"
                  v-if="!dataCtx.ship && !dataCtx.receive && dataCtx.shipment.status != 'Pending'" />
                <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
              </div>
            </div>
          </os-step>
        </os-steps>
      </div>
    </os-page-body>

    <os-confirm class="os-not-found-confirm" ref="notFoundConfirm">
      <template #title>
        <span v-t="'containers.add_containers.not_found'">Containers not found</span>
      </template>

      <template #message>
        <div class="message">
          <div v-t="'containers.add_containers.not_found_msg'">Following containers were not found: </div>

          <div><i>{{ctx.notFound.join(', ')}}</i></div>

          <div v-t="'containers.add_containers.proceed_q'">Do you want to proceed?</div>
        </div>
      </template>
    </os-confirm>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc    from '@/common/services/Alerts.js';
import exportSvc   from '@/common/services/ExportService.js';
import i18n        from '@/common/services/I18n.js';
import routerSvc   from '@/common/services/Router.js';
import util        from '@/common/services/Util.js';
import settingsSvc from '@/common/services/Setting.js';

import shipmentSvc from '@/administrative/services/Shipment.js';

import shipSpecimensSchema  from '@/administrative/schemas/shipments/ship-specimens.js';
import shipContainersSchema from '@/administrative/schemas/shipments/ship-containers.js';

export default {
  props: ['shipmentId', 'shipmentType', 'action', 'receive'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    const ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('ShipmentsList', {shipmentId: -1}), label: i18n.msg('shipments.list')}
      ],

      addEditFs: {rows: []},

      specimensSchema: {columns: []},

      containersSchema: {columns: []},

      loading: false,

      notFound: []
    });

    const dataCtx = reactive({
      shipment: {},

      currentUser: ui.currentUser,

      ship: props.action == 'ship',

      receive: props.receive,

      receivingInstitute: undefined,

      allowSpecimenRelabeling: false,

      allowExtIdName: false,

      allowExtIdValue: false,

      defaultExtIdName: undefined
    });

    return { ctx, dataCtx };
  },

  created: async function() {
    this.loadShipment();

    const allowRelabeling = await settingsSvc.getSetting('administrative', 'allow_spmn_relabeling');
    this.dataCtx.allowSpecimenRelabeling = util.isTrue(allowRelabeling[0].value);

    const addExtIds    = await settingsSvc.getSetting('administrative', 'add_spmn_ext_ids');
    const defExtIdName = await settingsSvc.getSetting('administrative', 'def_ext_id_name');
    this.dataCtx.allowExtIdName  = util.isTrue(addExtIds[0].value) && !defExtIdName[0].value;
    this.dataCtx.allowExtIdValue = util.isTrue(addExtIds[0].value);
    this.dataCtx.defaultExtIdName = defExtIdName[0].value;
  },

  computed: {
    validationReportMsgs: function() {
      return {
        label: i18n.msg(this.$refs.addSpmns && this.$refs.addSpmns.useBarcode ? 'specimens.barcode' : 'specimens.label'),
        error: i18n.msg('common.error'),
        notFound: i18n.msg('shipments.specimens_not_present'),
        extra: i18n.msg('shipments.extra_specimens')
      }
    },

    loadKey: function() {
      return this.shipmentId + ':' + this.shipmentType + ':' + this.receive;
    }
  },

  watch: {
    loadKey: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadShipment();
      }
    }
  },

  methods: {
    loadShipment: async function() {
      const ctx     = this.ctx;
      const dataCtx = this.dataCtx;

      ctx.loading = true;
      if (ctx.maxSpmnsLimit == null || ctx.maxSpmnsLimit == undefined) {
        const setting = await settingsSvc.getSetting('administrative', 'max_order_spmns_ui_limit');
        ctx.maxSpmnsLimit = +setting[0].value || 100;
      }

      let promises = [ shipmentSvc.getAddEditFormSchema() ];
      if (this.shipmentId && +this.shipmentId > 0) {
        promises.push(shipmentSvc.getShipment(+this.shipmentId));

        if (this.shipmentType == 'SPECIMEN') {
          promises.push(shipmentSvc.getSpecimens(+this.shipmentId, {startAt: 0, maxResults: ctx.maxSpmnsLimit + 1}));
        } else {
          promises.push(shipmentSvc.getContainers(+this.shipmentId, {startAt: 0, maxResults: 10000}));
        }
      } else {
        const shipment = dataCtx.shipment = {
          name: this._autoGenerateName({}),
          type: this.shipmentType || 'SPECIMEN',
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
        (result) => {
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

          if (this.ship && !dataCtx.shipment.shippedDate) {
            dataCtx.shipment.shippedDate = new Date();
          }

          if (this.receive) {
            dataCtx.shipment.receivedDate = new Date();

            if (dataCtx.shipment.type == 'SPECIMEN') {
              const defRecvQty = (dataCtx.specimenItems.length > ctx.maxSpmnsLimit) ? 'Acceptable' : null;
              dataCtx.specimenItems.forEach(
                item => {
                  item.specimen.storageLocation = null;
                  item.specimen.printLabel = (item.specimen.labelAutoPrintMode == 'ON_RECEIVE');
                  item.receivedQuality = item.receivedQuality || defRecvQty;
                }
              );
            } else {
              dataCtx.containerItems.forEach(item => item.container.storageLocation = null);
            }
          }

          ctx.specimensSchema  =  shipSpecimensSchema;
          ctx.containersSchema =  shipContainersSchema;
        },

        () => ctx.loading = false
      );
    },

    handleInput: function({field, value, data}) {
      const dataCtx = this.dataCtx;
      Object.assign(dataCtx, data);
      if (field.name == 'shipment.receivingInstitute' && dataCtx.receivingInstitute != value) {
        dataCtx.receivingInstitute = value;
        dataCtx.shipment.receivingSite = undefined;
        dataCtx.shipment.notifyUsers = [];
      } else if (field.name == 'shipment.request') {
        dataCtx.shipment.name = this._autoGenerateName(dataCtx.shipment);
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
      if (!this.dataCtx.specimenItems  || this.dataCtx.specimenItems.length == 0 || !this.$refs.specimenDetails) {
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
      const containers = await shipmentSvc.searchContainers(shipment, itemLabels);

      const notFound = [];
      for (let name of itemLabels) {
        let found = false;
        for (let container of containers) {
          if (container.name.toLowerCase() == name.toLowerCase()) {
            found = true;
            break;
          }
        }

        if (!found) {
          notFound.push(name);
        }
      }

      if (notFound.length > 0) {
        this.ctx.notFound = notFound;
        const resp = await this.$refs.notFoundConfirm.open();
        if (resp != 'proceed') {
          return;
        }
      }

      const toAdd      = containers.map(container => ({container: container}));
      util.addIfAbsent(this.dataCtx.containerItems, toAdd, 'container.id');
      this.$refs.addContainers.clearInput();
    },

    removeContainer: function({idx}) {
      this.dataCtx.containerItems.splice(idx, 1);
    },

    saveDraft: function() {
      this.saveOrUpdate('Pending').then(
        shipment => shipment && alertSvc.success({code: 'shipments.draft_saved', args: shipment})
      );
    },

    request: function() {
      this.saveOrUpdate('Requested').then(
        shipment => shipment && alertSvc.success({code: 'shipments.shipment_requested', args: shipment})
      );
    },

    ship: function() {
      this.saveOrUpdate('Shipped').then(
        shipment => shipment && alertSvc.success({code: 'shipments.shipped', args: shipment})
      );
    },

    receiveShipment: function() {
      this.saveOrUpdate('Received').then(
        shipment => shipment && alertSvc.success({code: 'shipments.received', args: shipment})
      );
    },

    updateShipment: function() {
      this.saveOrUpdate(null).then(
        shipment => shipment && alertSvc.success({code: 'shipments.updated', args: shipment})
      );
    },

    saveOrUpdate: async function(status) {
      if (this.dataCtx.shipment.type == 'SPECIMEN') {
        if (!this.dataCtx.specimenItems || this.dataCtx.specimenItems.length == 0) {
          alertSvc.error({code: 'shipments.no_specimens_add_one'});
          return;
        }

        if (this.$refs.specimenDetails && !this.$refs.specimenDetails.validate()) {
          return;
        }
      } else {
        if (!this.dataCtx.containerItems || this.dataCtx.containerItems.length == 0) {
          alertSvc.error({code: 'shipments.no_containers_add_one'});
          return;
        }

        if (!this.$refs.containerDetails.validate()) {
          return;
        }
      }

      const toSave = JSON.parse(JSON.stringify(this.dataCtx.shipment));
      if (toSave.type == 'SPECIMEN') {
        const items = toSave.shipmentSpmns = JSON.parse(JSON.stringify(this.dataCtx.specimenItems));
        if (status == 'Received') {
          for (let item of items) {
            const specimen = item.specimen;
            if (specimen.externalIdName || specimen.externalIdValue) {
              const externalIds = specimen.externalIds = specimen.externalIds || [];
              const name = specimen.externalIdName || this.dataCtx.defaultExtIdName;
              const externalId = externalIds.find(eid => eid.name == name);
              if (externalId) {
                externalId.value = specimen.externalIdValue;
              } else {
                externalIds.push({name, value: specimen.externalIdValue});
              }
            }
          }
        }
      } else {
        toSave.shipmentContainers = JSON.parse(JSON.stringify(this.dataCtx.containerItems));
      }

      if (status) {
        toSave.status = status;
      }

      const savedShipment = await shipmentSvc.saveOrUpdate(toSave);
      if (!toSave.id) {
        routerSvc.goto('ShipmentDetail.Overview', {shipmentId: savedShipment.id});
      } else {
        routerSvc.back();
      }

      return savedShipment;
    },

    cancel: function() {
      routerSvc.back();
    },

    exportShipment: function() {
      exportSvc.exportRecords({objectType: 'shipment', recordIds: [+this.shipmentId]});
    },


    _autoGenerateName: function(shipment) {
      const dateFmt = this.$ui.global.locale.shortDateFmt;
      const prefix = this.$t('shipments.' + ((!shipment || !shipment.request) ? 'shipment' : 'shipment_request'));
      return prefix.replaceAll(/\s+/g, '_') + '_' + util.formatDate(new Date(), dateFmt + '_HH:mm:ss');
    }
  }
}
</script>

<style scoped>
.os-not-found-confirm .message > div {
  padding: 0.5rem 0.25rem;
}
</style>
