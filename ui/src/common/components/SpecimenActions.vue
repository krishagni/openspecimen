
<template>
  <os-menu label="Actions" :options="options" />

  <os-confirm-delete ref="deleteSpecimensDialog" :captcha="false" :collectReason="true">
    <template #message>
      <span>Are you sure you want to delete the selected specimens and all its children?</span>
    </template>
  </os-confirm-delete>

  <os-close-specimen ref="closeSpecimensDialog" :specimens="specimens" />

  <os-dialog ref="distributeDialog">
    <template #header>
      <span>Distribution Details</span>
    </template>
    <template #content>
      <os-form ref="distDetailsForm" :data="ctx.distDetails" :schema="ctx.distFormSchema" />
    </template>
    <template #footer>
      <os-button text label="Cancel" @click="closeDistributionDialog" />
      <os-button primary label="Reserve" @click="reserveSpecimens" />
      <os-button primary label="Distribute" @click="showDistributeOptions" right-icon="caret-down" />
    </template>
  </os-dialog>

  <os-overlay class="os-dist-options" ref="distributeOptions">
    <template #default>
      <ul>
        <li>
          <a @click="distributeNow($event)">
            <span>Distribute Now</span>
          </a>
        </li>
        <li>
          <a @click="addDistributionDetails($event)">
            <span>Add More Details</span>
          </a>
        </li>
      </ul>
    </template>
  </os-overlay>

  <os-dialog ref="retrieveSpmnsDialog">
    <template #header>
      <span>Retrieve</span>
    </template>
    <template #content>
      <os-form ref="pickSpmnsForm" :data="ctx.retrieveDetails" :schema="ctx.pickSpmnsFormSchema" />
    </template>
    <template #footer>
      <os-button text label="Cancel" @click="closeRetrieveSpecimensDialog" />
      <os-button primary label="Retrieve" @click="retrieveSpecimens" />
    </template>
  </os-dialog>

  <os-plugin-views page="specimen-actions" view="menu" :viewProps="{cp: cp, cpr: cpr, visit: visit}" />
</template>

<script>

import specimenSvc from '@/biospecimen/services/Specimen.js';
import alertsSvc from '@/common/services/Alerts.js';
import authSvc   from '@/common/services/Authorization.js';
import extnsUtil from '@/common/services/ExtensionsUtil.js';
import http      from '@/common/services/HttpClient.js';
import itemsSvc  from '@/common/services/ItemsHolder.js';
import pluginReg from '@/common/services/PluginViewsRegistry.js';
import routerSvc from '@/common/services/Router.js';
import settingsSvc from '@/common/services/Setting.js';
import util      from '@/common/services/Util.js';

export default {

  props: ['cp', 'cpr', 'visit', 'specimens'],

  emits: ['reloadSpecimens'],

  data() {
    return {
      ctx: { 
        isCoordinator: false,

        isDistAllowed: false,

        distDetails: {},

        distFormSchema: {
          rows: [
            {
              fields: [
                {
                  type: 'dropdown',
                  label: 'Distribution Protocol',
                  name: 'dp',
                  listSource: {
                    loadFn: ({query, maxResults}) => this.loadDps(query, maxResults),
                    displayProp: 'shortTitle'
                  },
                  validations: {
                    required: {
                      message: 'Distribution protocol is mandatory'
                    }
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: 'textarea',
                  label: 'Comments',
                  name: 'comments',
                  rows: 5
                }
              ]
            },
            {
              fields: [
                {
                  type: 'radio',
                  label: 'Print Labels',
                  name: 'printLabel',
                  options: [
                    {caption: 'Yes', value: true},
                    {caption: 'No', value: false},
                  ],
                  optionsPerRow: 2
                }
              ]
            }
          ]
        },

        retrieveDetails: {},

        pickSpmnsFormSchema: {
          rows: [
            {
              fields: [
                {
                  type: 'datePicker',
                  label: 'Transfer Time',
                  showTime: true,
                  name: 'transferTime',
                  validation: {
                    required: {
                      message: 'Transfer time is mandatory'
                    }
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: 'textarea',
                  label: 'Comments',
                  name: 'comments'
                }
              ]
            }
          ]
        }
      }
    }
  },

  created() {
    if (this.cp) {
      settingsSvc.getSetting('biospecimen', 'coordinator_role_name').then(
        (settings) => {
          this.ctx.isCoordinator = settings[0].value == authSvc.getRole(this.cp);
        }
      );
    }

    if (authSvc.isAllowed({resource: 'Order', operations: ['Create']})) {
      if (this.cp) {
        if (this.cp.distributionProtocols && this.cp.distributionProtocols.length > 0) {
          this.ctx.isDistAllowed = true;
        } else {
          http.get('distribution-protocols/count', {cp: this.cp.shortTitle, excludeExpiredDps: true}).then(
            (resp) => {
              this.ctx.isDistAllowed = resp.count > 0;
            }
          );
        }
      } else {
        this.ctx.isDistAllowed = true;
      }
    }

  },

  computed: {
    options: function() {
      const isSpmnUpdateAllowed = authSvc.isAllowed({
        cp:         this.cp && this.cp.shortTitle,
        sites:      this.getSites(this.cp, this.cpr),
        resources:  ['Specimen', 'PrimarySpecimen'],
        operations: ['Update']
      });

      const isAllSpmnUpdateAllowed = authSvc.isAllowed({
        cp:         this.cp && this.cp.shortTitle,
        sites:      this.getSites(this.cp, this.cpr),
        resources:  ['Specimen'],
        operations: ['Update']
      });

      const isSpmnDeleteAllowed = authSvc.isAllowed({
        cp:         this.cp && this.cp.shortTitle,
        sites:      this.getSites(this.cp, this.cpr),
        resources:  ['Specimen', 'PrimarySpecimen'],
        operations: ['Delete']
      });

      const options = [];
      if (isSpmnUpdateAllowed) {
        options.push({ icon: 'edit', caption: 'Edit', onSelect: () => this.editSpecimens() });

        if (!this.ctx.isCoordinator) {
          options.push({ icon: 'print', caption: 'Print', onSelect: () => this.printLabels() });
        }
      }

      if (isSpmnDeleteAllowed) {
        options.push({ icon: 'trash', caption: 'Delete', onSelect: () => this.deleteSpecimens() });
      }

      if (isSpmnUpdateAllowed) {
        options.push({ icon: 'times', caption: 'Close', onSelect: () => this.closeSpecimens() });
      }

      if (this.ctx.isDistAllowed) {
        options.push({ icon: 'share', caption: 'Distribute', onSelect: () => this.showDistributionDialog() });
      }

      if (authSvc.isAllowed({resource: 'ShippingAndTracking', operations: ['Create']})) {
        options.push({ icon: 'paper-plane', caption: 'Ship', onSelect: () => this.shipSpecimens() });
      }

      if (isSpmnUpdateAllowed && !this.ctx.isCoordinator) {
        options.push({ icon: 'check-square', caption: 'Receive', onSelect: () => this.receiveSpecimens() });
      }

      if (isAllSpmnUpdateAllowed) {
        options.push({ icon: 'flask', caption: 'Pool', onSelect: () => this.poolSpecimens() });
        options.push({ icon: 'share-alt', caption: 'Create Aliquots', onSelect: () => this.createAliquots() });
        options.push({ icon: 'flask', caption: 'Create Derivatives', onSelect: () => this.createDerivatives() });
      }

      if (isSpmnUpdateAllowed && !this.ctx.isCoordinator) {
        options.push({ icon: 'calendar', caption: 'Add/Edit Event', onSelect: () => this.addEditEvent() });
      }

      if (isSpmnUpdateAllowed && authSvc.isAllowed({resource: 'StorageContainer', operations: ['Read']})) {
        options.push({ icon: 'arrows-alt-h', caption: 'Transfer', onSelect: () => this.transferSpecimens() });
        options.push({ icon: 'reply', caption: 'Retrieve', onSelect: () => this.showRetrieveSpecimensDialog() });
      }

      const pluginOptions = pluginReg.getOptions('specimen-actions', 'menu');
      const ctxt = {
        cp: this.cp, cpr: this.cpr, visit: this.visit,
        isSpmnUpdateAllowed, isAllSpmnUpdateAllowed, isSpmnDeleteAllowed,
        isCoordinator: this.ctx.isCoordinator
      };

      for (let option of pluginOptions) {
        if (typeof option.showIf == 'function' && !option.showIf(ctxt)) {
          continue;
        }

        options.push({ icon: option.icon, caption: option.caption, onSelect: () => option.exec(ctxt, this.specimens) });
      }

      return options;
    }
  },

  methods: {
    getSites: function(cp, cpr) {
      let sites = [];
      if (cp) {
        sites = cp.cpSites.map(cpSite => cpSite.siteName);
        if (this.$ui.global.appProps.mrn_restriction_enabled && cpr) {
          if (cpr.participant && cpr.participant.pmis) {
            cpr.participant.pmis.forEach(pmi => sites.push(pmi.siteName));
          }
        }
      }

      return sites;
    },

    editSpecimens: function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error('Please select at least one existing specimen to edit');
        return;
      }

      if (specimens.length > 100) {
        alertsSvc.error(specimens.length + ' specimens selected. Only 100 specimens can be edited in bulk!');
        return;
      }

      itemsSvc.ngSetItems('specimens', specimens);
      routerSvc.ngGoto('bulk-edit-specimens');
    },

    printLabels: function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error('Please select at least one existing specimen for label printing');
        return;
      }

      const parts = [util.formatDate(new Date(), 'yyyyMMdd_HHmmss')];
      if (this.cpr) {
        parts.unshift(this.cpr.ppid);
        parts.unshift(this.cpr.cpShortTitle);
      } else if (this.cp) {
        parts.unshift(this.cp.shortTitle);
      }

      const outputFilename = parts.join('_') + '.csv';
      const specimenIds = this.specimens.map(spmn => spmn.id);
      specimenSvc.printLabels({specimenIds}, outputFilename);
    },

    deleteSpecimens: function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error('Please select at least one existing specimen to delete');
        return;
      }

      this.$refs.deleteSpecimensDialog.open().then(
        ({reason}) => {
          specimenSvc.bulkDelete(specimens.map(({id}) => id), reason).then(
            () => {
              alertsSvc.success('Selected specimens and their children successfully deleted');
              this.$emit('reloadSpecimens');
            }
          );
        }
      );
    },

    closeSpecimens: function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error('Please select at least one collected specimen to close');
        return;
      }

      this.$refs.closeSpecimensDialog.open().then(
        () => {
          alertsSvc.success('Selected specimens successfully closed');
          this.$emit('reloadSpecimens');
        }
      );
    },

    shipSpecimens: function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error('Please select at least one collected specimen to create shipment');
        return;
      }

      localStorage.selectedSpecimenIds = JSON.stringify(specimens.map(spmn => spmn.id));
      routerSvc.goto('ShipmentAddEdit', {shipmentId: -1, shipmentType: 'SPECIMEN'});
    },

    receiveSpecimens: async function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        routerSvc.ngGoto('receive-specimens');
        return;
      }

      const ids = specimens.map(spmn => spmn.id);
      const dbSpmns = await specimenSvc.getByIds(ids, true);

      const nonPrimarySpmns = dbSpmns.filter(spmn => spmn.lineage != 'New');
      if (nonPrimarySpmns.length > 0) {
        this.showError('One or more specimens are not primary specimens. Non-primary specimens:', nonPrimarySpmns);
        return;
      }

      const closedSpmns = dbSpmns.filter(spmn => spmn.activityStatus != 'Active');
      if (closedSpmns.length > 0) {
        this.showError('Closed specimens cannot be edited. Closed specimens:', closedSpmns);
        return;
      }

      const ncSpmns = dbSpmns.filter(spmn => spmn.status != 'Collected');
      if (ncSpmns.length > 0) {
        this.showError('One or more specimens are not collected. Not collected specimens:', ncSpmns);
        return;
      }

      const rcvdSpmns = dbSpmns.filter(spmn => spmn.receivedEvent.receivedQuality != 'To be Received');
      if (rcvdSpmns.length > 0) {
        this.showError('One or more specimens have been already received. Received specimens:', rcvdSpmns);
        return;
      }

      for (let spmn of dbSpmns) {
        extnsUtil.createExtensionFieldMap(spmn, true);
      }

      itemsSvc.ngSetItems('specimens', dbSpmns);
      routerSvc.ngGoto('receive-specimens');
    },

    poolSpecimens: async function() {
      const ids = this.specimens.map(spmn => spmn.id);
      if (ids.length < 2) { 
        alertsSvc.error('Select two or more available specimens to create a pooled specimen.');
        return;
      }

      const dbSpmns = await specimenSvc.getByIds(ids, false);

      const ncSpmns = dbSpmns.filter(spmn => spmn.status != 'Collected');
      if (ncSpmns.length > 0) {
        this.showError('One or more specimens are not collected. Not collected specimens:', ncSpmns);
        return;
      } 

      const closedSpmns = dbSpmns.filter(spmn => spmn.activityStatus != 'Active');
      if (closedSpmns.length > 0) {
        this.showError('Closed specimens cannot be edited. Closed specimens:', closedSpmns);
        return;
      }

      const cpId = dbSpmns[0].cpId;
      if (dbSpmns.some(spmn => spmn.cpId != cpId)) {
        alertsSvc.error('Select specimens of the same collection protocol.');
        return;
      }

      const cprId   = this.cpr && this.cpr.id;
      const visitId = this.visit && this.visit.id;
      itemsSvc.ngSetItems('specimens', dbSpmns);
      routerSvc.ngGoto('cp-view/' + cpId + '/add-pooled-specimen', {cprId: cprId, visitId: visitId});
    },

    showDistributionDialog: function() {
      if (!this.specimens || this.specimens.length == 0) {
        alertsSvc.error('Please select at least one collected specimen to distribute');
        return;
      }

      this.ctx.distDetails = {};
      this.$refs.distributeDialog.open();
    },

    closeDistributionDialog: function() {
      this.ctx.distDetails = {};
      this.$refs.distributeDialog.close();
    },

    reserveSpecimens: function() {
      if (!this.$refs.distDetailsForm.validate()) {
        return;
      }

      const userInput = this.ctx.distDetails;
      const request = {
        dpId: userInput.dp.id,
        comments: userInput.comments,
        specimens: this.specimens.map(spmn => ({id: spmn.id }))
      };

      http.put('distribution-protocols/' + userInput.dp.id + '/reserved-specimens', request).then(
        ({updated}) => {
          if (updated == 0) {
            alertsSvc.info('Specimens are already reserved for distribution to the selected DP');
          } else if (updated == 1) {
            alertsSvc.success('Specimen reserved');
          } else {
            alertsSvc.success(updated + ' specimens reserved');
          }

          this.closeDistributionDialog();
          this.$emit('reloadSpecimens');
        }
      );
    },

    showDistributeOptions: function(event) {
      this.$refs.distributeOptions.toggle(event);
    },

    distributeNow: function() {
      this.$refs.distributeOptions.toggle(event);

      const userInput = this.ctx.distDetails;
      const dp = userInput.dp;
      const order = {
        name: dp.shortTitle + '_' + util.formatDate(new Date(), this.$ui.global.locale.shortDateFmt + '_HH:mm:ss'),
        distributionProtocol: dp,
        requester: dp.principalInvestigator,
        siteName: dp.defReceivingSiteName,
        distributeAvailableQty: true,
        orderItems: this.getOrderItems(this.specimens, userInput.printLabel),
        comments: userInput.comments,
        status: 'EXECUTED'
      };

      http.post('distribution-orders', order).then(
        (createdOrder) => {
          if (createdOrder.completed) {
            alertsSvc.success('Distribution order ' + createdOrder.name + ' created successfully');
          } else {
            alertsSvc.info('Saving distribution order is taking more time than anticipated. An email notification will be sent to you on successful distribution of specimens');
          }

          this.$emit('reloadSpecimens');
          this.closeDistributionDialog();
        }
      );
    },

    addDistributionDetails: function(event) {
      this.$refs.distributeOptions.toggle(event);

      const userInput = this.ctx.distDetails;
      localStorage['os.orderDetails'] = JSON.stringify({
        dp: userInput.dp,
        specimenIds: this.specimens && this.specimens.map(spmn => spmn.id),
        printLabel: userInput.printLabel,
        comments: userInput.comments
      });

      this.$goto('OrderAddEdit', {orderId: -1});
    },

    loadDps: function(query) {
      return new Promise(
        (resolve) => {
          let cpShortTitle;
          if (this.cp) {
            if (this.cp.distributionProtocols && this.cp.distributionProtocols.length > 0) {
              const dps = this.cp.distributionProtocols;
              if (!this.ctx.defDps) {
                this.ctx.defDps = dps;
              }

              resolve(dps);
              return;
            }

            cpShortTitle = this.cp.shortTitle;
          }

          if (this.ctx.defDps && (!query || this.ctx.defDps.length < 100)) {
            resolve(this.ctx.defDps);
            return;
          }

          const filterOpts = {activityStatus: 'Active', query: query, excludeExpiredDps: true, cp: cpShortTitle};
          http.get('distribution-protocols', filterOpts).then(
            (dps) => {
              if (!query && !this.ctx.defDps) {
                this.ctx.defDps = dps;
              }

              resolve(dps);
            }
          );
        }
      );
    },

    getOrderItems: function(specimens, printLabel) {
      return (specimens || []).map(
        (specimen) => ({
          specimen: specimen,
          quantity: specimen.availableQty,
          status: 'DISTRIBUTED_AND_CLOSED',
          printLabel: printLabel
        })
      );
    },

    createAliquots: function() {
      this.gotoNgView('bulk-create-aliquots', {}, 'Select at least one collected parent specimen to create aliquots');
    },

    createDerivatives: function() {
      this.gotoNgView('bulk-create-derivatives', {}, 'Select at least one collected parent specimen to create derived specimens');
    },

    addEditEvent: function() {
      this.gotoNgView('bulk-add-event', {}, 'Select at least one collected specimen to add/edit event');
    },

    transferSpecimens: function() {
      this.gotoNgView('bulk-transfer-specimens', {}, 'Select at least one specimen to transfer');
    },

    showRetrieveSpecimensDialog: function() {
      if (!this.specimens || this.specimens.length == 0) {
        alertsSvc.error('Select at least one collected specimen to retrieve');
        return;
      }

      this.ctx.retrieveDetails = { transferTime: Date.now() };
      this.$refs.retrieveSpmnsDialog.open();
    },

    closeRetrieveSpecimensDialog: function() {
      this.$refs.retrieveSpmnsDialog.close();
    },

    retrieveSpecimens: function() {
      const input = this.ctx.retrieveDetails;
      const spmns = this.specimens.map(
        (spmn) => {
          return {
            id: spmn.id,
            storageLocation: {},
            transferTime: input.transferTime,
            transferComments: input.comments
          }
        }
      );

      specimenSvc.bulkUpdate(spmns).then(
        () => {
          this.$emit('reloadSpecimens');
          this.closeRetrieveSpecimensDialog();
        }
      );
    },

    showError: function(error, spmns) {
      alertsSvc.error(error + ' ' + spmns.map(s => !s.label ? s.id : s.label).join(', '));
    },

    gotoNgView: async function(url, params, msg, anyStatus, excludeExtensions) {
      if (!this.specimens || this.specimens.length == 0) {
        alertsSvc.error(msg);
        return;
      }

      const spmnIds = this.specimens.map(spmn => spmn.id);
      const dbSpmns = await specimenSvc.getByIds(spmnIds, excludeExtensions != true);

      if (!anyStatus) {
        const ncSpmns = dbSpmns.filter(spmn => spmn.status != 'Collected');
        if (ncSpmns.length > 0) {
          this.showError('One or more specimens are not collected. Not collected specimens:', ncSpmns);
          return;
        }

        const closedSpmns = dbSpmns.filter(spmn => spmn.activityStatus != 'Active');
        if (closedSpmns.length > 0) {
          this.showError('Closed specimens cannot be edited. Closed specimens:', closedSpmns);
          return;
        }
      }

      for (let spmn of dbSpmns) {
        extnsUtil.createExtensionFieldMap(spmn, true);
      }

      itemsSvc.ngSetItems('specimens', dbSpmns);
      routerSvc.ngGoto(url, params);
    }
  }
}
</script>

<style scoped>
.os-dist-options ul {
  margin: -1.25rem;
  padding: 0;
  list-style: none;
}

.os-dist-options ul li a {
  padding: 0.75rem 1rem;
  text-decoration: none;
  display: inline-block;
  width: 100%;
  color: #212529;
}

.os-dist-options ul li a:hover {
  background: #e9ecef;
}
</style>
