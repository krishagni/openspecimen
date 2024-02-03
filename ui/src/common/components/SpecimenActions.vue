<template>
  <os-menu :label="$t('common.specimen_actions.title')" :options="options" />

  <os-confirm-delete ref="deleteSpecimensDialog" :captcha="false" :collectReason="true">
    <template #message>
      <span v-t="'common.specimen_actions.delete_selected'">Are you sure you want to delete the selected specimens and all its children?</span>
    </template>
  </os-confirm-delete>

  <os-close-specimen ref="closeSpecimensDialog" :specimens="specimens" />

  <os-dialog ref="distributeDialog">
    <template #header>
      <span v-t="'common.specimen_actions.distribution_details'">Distribution Details</span>
    </template>
    <template #content>
      <os-form ref="distDetailsForm" :data="ctx.distDetails" :schema="ctx.distFormSchema" />
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')"     @click="closeDistributionDialog" />
      <os-button primary :label="$t('common.specimen_actions.reserve')"    @click="reserveSpecimens" />
      <os-button primary :label="$t('common.specimen_actions.distribute')" @click="showDistributeOptions" right-icon="caret-down" />
    </template>
  </os-dialog>

  <os-overlay class="os-dist-options" ref="distributeOptions">
    <template #default>
      <ul>
        <li>
          <a @click="distributeNow($event)">
            <span v-t="'common.specimen_actions.distribute_now'">Distribute Now</span>
          </a>
        </li>
        <li>
          <a @click="addDistributionDetails($event)">
            <span v-t="'common.specimen_actions.add_more_details'">Add More Details</span>
          </a>
        </li>
      </ul>
    </template>
  </os-overlay>

  <os-dialog ref="retrieveSpmnsDialog">
    <template #header>
      <span v-t="'common.specimen_actions.retrieve'">Retrieve</span>
    </template>
    <template #content>
      <os-form ref="pickSpmnsForm" :data="ctx.retrieveDetails" :schema="ctx.pickSpmnsFormSchema" />
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')"   @click="closeRetrieveSpecimensDialog" />
      <os-button primary :label="$t('common.specimen_actions.retrieve')" @click="retrieveSpecimens" />
    </template>
  </os-dialog>

  <os-plugin-views ref="pluginViews" page="specimen-actions" view="menu"
    :viewProps="{cp: cp, cpr: cpr, visit: visit}" />
</template>

<script>

import cpSvc       from '@/biospecimen/services/CollectionProtocol.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import wfSvc       from '@/biospecimen/services/Workflow.js';

import alertsSvc   from '@/common/services/Alerts.js';
import authSvc     from '@/common/services/Authorization.js';
import extnsUtil   from '@/common/services/ExtensionsUtil.js';
import http        from '@/common/services/HttpClient.js';
import itemsSvc    from '@/common/services/ItemsHolder.js';
import pluginReg   from '@/common/services/PluginViewsRegistry.js';
import routerSvc   from '@/common/services/Router.js';
import settingsSvc from '@/common/services/Setting.js';
import util        from '@/common/services/Util.js';

export default {
  props: ['cp', 'cpr', 'visit', 'specimens', 'cartId'],

  emits: ['reloadSpecimens'],

  data() {
    return {
      ctx: { 
        isCoordinator: false,

        isDistAllowed: false,

        distDetails: {
          clearListId: this.cartId,
          clearListMode: 'NONE'
        },

        distFormSchema: {
          rows: [
            {
              fields: [
                {
                  type: 'dropdown',
                  labelCode: 'dps.singular',
                  name: 'dp',
                  listSource: {
                    loadFn: ({query, maxResults}) => this.loadDps(query, maxResults),
                    displayProp: 'shortTitle'
                  },
                  validations: {
                    required: {
                      messageCode: 'dps.dp_req'
                    }
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: 'textarea',
                  labelCode: 'dps.comments',
                  name: 'comments',
                  rows: 5
                }
              ]
            },
            {
              fields: [
                {
                  type: 'radio',
                  labelCode: 'dps.remove_from_cart',
                  name: 'clearListMode',
                  options: [
                    {caption: this.$t('dps.distributed_specimens'), value: 'DISTRIBUTED'},
                    {caption: this.$t('dps.all_specimens'), value: 'ALL'},
                    {caption: this.$t('dps.none'), value: 'NONE'}
                  ],
                  optionsPerRow: 3,
                  validations: {
                    required: {
                      messageCode: 'dps.remove_from_cart_req'
                    }
                  },
                  showWhen: 'clearListId != undefined && clearListId != null'
                }
              ]
            },
            {
              fields: [
                {
                  type: 'radio',
                  labelCode: 'dps.checkout_specimens',
                  name: 'checkout',
                  options: [
                    {caption: this.$t('common.yes'), value: true},
                    {caption: this.$t('common.no'), value: false}
                  ],
                  optionsPerRow: 3
                }
              ]
            },
            {
              fields: [
                {
                  type: 'radio',
                  labelCode: 'dps.print_labels',
                  name: 'printLabel',
                  options: [
                    {caption: this.$t('common.yes'), value: true},
                    {caption: this.$t('common.no'),  value: false},
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
                  type: 'user',
                  labelCode: 'common.specimen_actions.transfer_user',
                  name: 'transferUser',
                  validations: {
                    required: {
                      messageCode: 'common.specimen_actions.transfer_user_req'
                    }
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: 'datePicker',
                  labelCode: 'common.specimen_actions.transfer_time',
                  showTime: true,
                  name: 'transferTime',
                  validations: {
                    required: {
                      messageCode: 'common.specimen_actions.transfer_time_req'
                    }
                  }
                }
              ]
            },
            {
              fields: [
                {
                  type: 'textarea',
                  labelCode: 'common.comments',
                  name: 'comments'
                }
              ]
            },
            {
              fields: [
                {
                  type: 'booleanCheckbox',
                  inlineLabelCode: 'common.specimen_actions.checkout',
                  name: 'checkout'
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

    cpSvc.getWorkflowProperty(-1, 'common', 'receiveSpecimensWorkflow')
      .then(wfId => this.ctx.recvSpmnsWfId = wfId);
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

      const i18n = this.$t;
      const options = [];
      if (isSpmnUpdateAllowed) {
        options.push({ icon: 'edit', caption: i18n('common.buttons.edit'), onSelect: () => this.editSpecimens() });

        if (!this.ctx.isCoordinator) {
          options.push({ icon: 'print', caption: i18n('common.buttons.print'), onSelect: () => this.printLabels() });
        }
      }

      if (isSpmnDeleteAllowed) {
        options.push({ icon: 'trash', caption: i18n('common.buttons.delete'), onSelect: () => this.deleteSpecimens() });
      }

      if (isSpmnUpdateAllowed) {
        options.push({ icon: 'times', caption: i18n('common.buttons.close'), onSelect: () => this.closeSpecimens() });
      }

      if (this.ctx.isDistAllowed) {
        options.push({ icon: 'share', caption: i18n('common.specimen_actions.distribute'), onSelect: () => this.showDistributionDialog() });
        if (+this.cartId > 0) {
          options.push({ icon: 'share', caption: i18n('common.specimen_actions.distribute_all'), onSelect: () => this.showDistributionDialog(true) });
        }
      }

      if (authSvc.isAllowed({resource: 'ShippingAndTracking', operations: ['Create']})) {
        options.push({ icon: 'paper-plane', caption: i18n('common.specimen_actions.ship'), onSelect: () => this.shipSpecimens() });
      }

      if (isSpmnUpdateAllowed && !this.ctx.isCoordinator && this.ctx.recvSpmnsWfId > 0) {
        options.push({ icon: 'check-square', caption: i18n('common.specimen_actions.receive'), onSelect: () => this.receiveSpecimens() });
      }

      if (isAllSpmnUpdateAllowed) {
        options.push({ icon: 'flask', caption: i18n('common.specimen_actions.pool'), onSelect: () => this.poolSpecimens() });
        options.push({ icon: 'share-alt', caption: i18n('common.specimen_actions.create_aliquots'), onSelect: () => this.createAliquots() });
        options.push({ icon: 'flask', caption: i18n('common.specimen_actions.create_derivatives'), onSelect: () => this.createDerivatives() });
      }

      if (isSpmnUpdateAllowed && !this.ctx.isCoordinator) {
        options.push({ icon: 'calendar', caption: i18n('common.specimen_actions.add_edit_event'), onSelect: () => this.addEditEvent() });
      }

      if (isSpmnUpdateAllowed && authSvc.isAllowed({resource: 'StorageContainer', operations: ['Read']})) {
        options.push({ icon: 'arrows-alt-h', caption: i18n('common.specimen_actions.transfer'), onSelect: () => this.transferSpecimens() });
        options.push({ icon: 'reply', caption: i18n('common.specimen_actions.retrieve'), onSelect: () => this.showRetrieveSpecimensDialog() });
      }

      const pluginOptions = pluginReg.getOptions('specimen-actions', 'menu');
      const ctxt = {
        cp: this.cp, cpr: this.cpr, visit: this.visit,
        isSpmnUpdateAllowed, isAllSpmnUpdateAllowed, isSpmnDeleteAllowed,
        isCoordinator: this.ctx.isCoordinator, menu: this
      };

      for (let option of pluginOptions) {
        if (typeof option.showIf == 'function' && !option.showIf(ctxt)) {
          continue;
        }

        options.push({
          icon: option.icon,
          caption: option.caption,
          onSelect: () => {
            option.exec(this.wrapRefs(ctxt), this.specimens)
          }
        });
      }

      const style = {'padding-top': '0.3rem!important', 'padding-bottom': '0.3rem!important'};
      options.forEach(option => option.anchorStyle = style);
      return options;
    }
  },

  methods: {
    wrapRefs: function(ctxt) {
      return {...ctxt, pluginViews: this.$refs.pluginViews};
    },

    getSites: function(cp, cpr) {
      let sites = null;
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

    editSpecimens: async function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error({code: 'common.specimen_actions.select_for_edit'});
        return;
      }

      const settings = await settingsSvc.getSetting('biospecimen', 'max_spmns_update_limit')
      const limit = +(settings[0].value || 100);
      if (specimens.length > limit) {
        alertsSvc.error({code: 'common.specimen_actions.edit_max_limit', args: {count: specimens.length, limit}});
        return;
      }

      itemsSvc.ngSetItems('specimens', specimens);
      routerSvc.ngGoto('bulk-edit-specimens');
    },

    printLabels: function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error({code: 'common.specimen_actions.select_for_print'});
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
        alertsSvc.error({code: 'common.specimen_actions.select_for_delete'});
        return;
      }

      this.$refs.deleteSpecimensDialog.open().then(
        ({reason}) => {
          specimenSvc.bulkDelete(specimens.map(({id}) => id), reason).then(
            () => {
              alertsSvc.error({code: 'common.specimen_actions.deleted'});
              this.$emit('reloadSpecimens');
            }
          );
        }
      );
    },

    closeSpecimens: function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error({code: 'common.specimen_actions.select_for_close'});
        return;
      }

      this.$refs.closeSpecimensDialog.open().then(
        () => {
          alertsSvc.error({code: 'common.specimen_actions.closed'});
          this.$emit('reloadSpecimens');
        }
      );
    },

    shipSpecimens: function() {
      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        alertsSvc.error({code: 'common.specimen_actions.select_for_shipment'});
        return;
      }

      localStorage.selectedSpecimenIds = JSON.stringify(specimens.map(spmn => spmn.id));
      routerSvc.goto('ShipmentAddEdit', {shipmentId: -1, shipmentType: 'SPECIMEN'});
    },

    receiveSpecimens: async function() {
      if (!(this.ctx.recvSpmnsWfId > 0)) {
        alertsSvc.error({code: 'specimens.receive_wf_not_configured'});
        return;
      }

      const specimens = this.specimens;
      if (!specimens || specimens.length == 0) {
        routerSvc.goto('tmWorkflowCreateInstance', {workflowId: this.ctx.recvSpmnsWfId});
        return;
      }

      const payload = {
        workflow: {id: this.ctx.recvSpmnsWfId},
        inputItems: specimens.map(specimen => ({specimen: {id: specimen.id}}))
      };
      http.post('workflow-instances', payload).then(
        (instance) => {
          routerSvc.goto('tmWorkflowInstance', {wfInstanceId: instance.id});
        }
      );
    },

    poolSpecimens: async function() {
      const ids = (this.specimens || []).map(spmn => spmn.id);
      if (ids.length < 2) { 
        alertsSvc.error({code: 'common.specimen_actions.select_for_pooling'});
        return;
      }

      const dbSpmns = await specimenSvc.getByIds(ids, false);

      const ncSpmns = dbSpmns.filter(spmn => spmn.status != 'Collected');
      if (ncSpmns.length > 0) {
        this.showError('common.specimen_actions.not_collected', ncSpmns);
        return;
      } 

      const closedSpmns = dbSpmns.filter(spmn => spmn.activityStatus != 'Active');
      if (closedSpmns.length > 0) {
        this.showError('common.specimen_actions.cannot_op_closed', closedSpmns);
        return;
      }

      const cpId = dbSpmns[0].cpId;
      if (dbSpmns.some(spmn => spmn.cpId != cpId)) {
        alertsSvc.error({code: 'common.specimen_actions.select_same_cp_specimens'});
        return;
      }

      wfSvc.createPooledSpecimens(dbSpmns);
    },

    showDistributionDialog: function(distAll) {
      if (!distAll && (!this.specimens || this.specimens.length == 0)) {
        alertsSvc.error({code: 'common.specimen_actions.select_for_distribution'});
        return;
      }

      this.ctx.distDetails = {
        specimenListId: (distAll && this.cartId) || undefined,
        clearListId: this.cartId,
        clearListMode: 'NONE'
      };
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
            alertsSvc.error({code: 'common.specimen_actions.already_reserved_for_dp'});
          } else {
            alertsSvc.success({code: 'common.specimen_actions.reserved', args: {count: updated}});
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
      if (!this.$refs.distDetailsForm.validate()) {
        return;
      }

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
        specimenList: userInput.specimenListId && {id: userInput.specimenListId},
        clearListId: userInput.clearListId,
        clearListMode: userInput.clearListMode,
        checkout: userInput.checkout,
        status: 'EXECUTED'
      };

      http.post('distribution-orders', order).then(
        (createdOrder) => {
          if (createdOrder.completed) {
            alertsSvc.success({code: 'common.specimen_actions.order_created', args: createdOrder});
          } else {
            alertsSvc.info({code: 'common.specimen_actions.order_taking_time'});
          }

          this.$emit('reloadSpecimens');
          this.closeDistributionDialog();
        }
      );
    },

    addDistributionDetails: function(event) {
      if (!this.$refs.distDetailsForm.validate()) {
        return;
      }

      this.$refs.distributeOptions.toggle(event);

      const userInput = this.ctx.distDetails;
      localStorage['os.orderDetails'] = JSON.stringify({
        dp: userInput.dp,
        specimenIds: this.specimens && this.specimens.map(spmn => spmn.id),
        printLabel: userInput.printLabel,
        specimenListId: userInput.specimenListId,
        clearListId: userInput.clearListId,
        clearListMode: userInput.clearListMode,
        checkout: userInput.checkout,
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
      this.gotoNgView('bulk-create-aliquots', {}, 'common.specimen_actions.select_for_aliquots', false, false, true);
    },

    createDerivatives: function() {
      this.gotoNgView('bulk-create-derivatives', {}, 'common.specimen_actions.select_for_derived', false, false, true);
    },

    addEditEvent: function() {
      this.gotoNgView('bulk-add-event', {}, 'common.specimen_actions.select_for_add_edit_event', false, false, false);
    },

    transferSpecimens: function() {
      this.gotoNgView('bulk-transfer-specimens', {}, 'common.specimen_actions.select_for_transfer', false, false, true);
    },

    showRetrieveSpecimensDialog: function() {
      if (!this.specimens || this.specimens.length == 0) {
        alertsSvc.error({code: 'common.specimen_actions.select_for_retrieve'});
        return;
      }

      this.ctx.retrieveDetails = { transferUser: this.$ui.currentUser, transferTime: Date.now() };
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
            transferUser: input.transferUser,
            transferTime: input.transferTime,
            transferComments: input.comments,
            checkout: input.checkout
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
      alertsSvc.error({code: error, args: {labels: spmns.map(s => !s.label ? s.id : s.label).join(', ')}});
    },

    gotoNgView: async function(url, params, msg, anyStatus, excludeExtensions, forbidClosedSpmns) {
      if (!this.specimens || this.specimens.length == 0) {
        alertsSvc.error({code: msg});
        return;
      }

      const spmnIds = this.specimens.map(spmn => spmn.id);
      const dbSpmns = await specimenSvc.getByIds(spmnIds, excludeExtensions != true);

      if (!anyStatus) {
        const ncSpmns = dbSpmns.filter(spmn => spmn.status != 'Collected');
        if (ncSpmns.length > 0) {
          this.showError('common.specimen_actions.not_collected', ncSpmns);
          return;
        }

        if (forbidClosedSpmns) {
          const closedSpmns = dbSpmns.filter(spmn => spmn.activityStatus != 'Active');
          if (closedSpmns.length > 0) {
            this.showError('common.specimen_actions.cannot_op_closed', closedSpmns);
            return;
          }
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
