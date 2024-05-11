
<template>
  <os-page-toolbar>
    <template #default>
      <span v-if="specimen.id > 0">
        <os-button left-icon="edit" :label="$t('common.buttons.edit')"
          @click="edit" v-if="isAnyUserUpdateAllowed" />

        <os-menu icon="plus" :label="$t('common.buttons.create')"
          :options="[
            {icon: 'flask',     caption: $t('specimens.derived'),  onSelect: createDerivatives},
            {icon: 'share-alt', caption: $t('specimens.aliquots'), onSelect: createAliquots}
          ]"

          v-if="specimen.availabilityStatus == 'Available' && isCreateChildrenAllowed"
        />

        <os-button left-icon="print" :label="$t('common.buttons.print')"
          @click="confirmPrint" v-if="isPrintAllowed" />

        <os-add-to-cart :specimens="[{id: specimen.id}]" />

        <os-menu left-icon="plus" :label="$t('specimens.add_event')" :options="ctx.eventForms"
          @menu-toggled="loadEventForms"
          v-if="specimen.availabilityStatus == 'Available' && isUpdateAllowed" />

        <os-menu :label="$t('common.buttons.more')" :options="ctx.moreOptions" v-if="ctx.moreOptions.length > 0"/>
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />

      <SpecimenTree :cp="ctx.cp" :cpr="cpr" :visit="visit" :specimens="ctx.children"
        :ref-date="ctx.specimen.status && ctx.specimen.status != 'Pending' ? ctx.specimen.createdOn : 0"
        @reload="reloadChildren" v-if="ctx.cp.id > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.auditObjs" v-if="ctx.specimen.id > 0" />

      <os-section v-if="ctx.events && ctx.events.length > 0">
        <template #title>
          <span v-t="'specimens.recent_activity'">Recent Activity</span>
        </template>

        <template #content>
          <EventsSummaryList :events="ctx.events" :hide-actions="!isUpdateAllowed"
            @click="showEvent($event)" @edit-event="editEvent($event)" @delete-event="deleteEvent($event)" />
        </template>
      </os-section>
    </os-grid-column>
  </os-grid>

  <os-dialog ref="printDialog">
    <template #header>
      <span v-t="'specimens.confirm_print'">Confirm Print</span>
    </template>

    <template #content>
      <span v-t="'specimens.print_child_labels_q'">Do you want to print child specimen labels as well?</span>
    </template>

    <template #footer>
      <os-button text      :label="$t('common.buttons.cancel')"  @click="cancelPrint" />
      <os-button secondary :label="$t('specimens.no_print_current_specimen')" @click="printLabels(false)" />
      <os-button primary   :label="$t('common.buttons.yes')" @click="printLabels(true)" />
    </template>
  </os-dialog>

  <os-delete-object ref="deleteSpmnDialog" :input="ctx.deleteOpts" />

  <os-close-specimen ref="closeSpmnDialog" :specimens="[ctx.specimen]" />

  <os-dialog ref="eventOverviewDialog">
    <template #header>
      <span>{{ctx.event.name}}: #{{ctx.event.id}}</span>
    </template>
    <template #content>
      <os-form-record-overview :record="ctx.eventRecord" />
    </template>
    <template #footer>
      <os-button text   :label="$t('common.buttons.close')" @click="closeEventOverview()" />
      <os-plugin-views page="specimen-detail" view="event-overview" :view-props="pluginViewProps" />
      <os-button danger left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteEvent(ctx.event)"
        v-if="!ctx.event.sysForm && isUpdateAllowed" />
      <os-button primary left-icon="edit" :label="$t('common.buttons.edit')" @click="editEvent(ctx.event)"
        v-if="ctx.event.isEditable && isUpdateAllowed" />
    </template>
  </os-dialog>

  <os-confirm ref="eventDeleteConfirmDialog">
    <template #title>
      <span v-t="'common.delete_confirmation'">Confirm Delete</span>
    </template>
    <template #message>
      <span v-t="{path: 'specimens.delete_event_q', args: ctx.event}"></span>
    </template>
  </os-confirm>

  <os-dialog ref="transferDialog">
    <template #header>
      <span v-t="transferCtx.checkout ? 'specimens.checkout_specimen' : 'specimens.checkin_specimen'"></span>
    </template>
    <template #content>
      <os-form ref="transferForm" :schema="transferSchema.layout" :data="transferCtx" />
    </template>
    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="cancelTransfer" />

      <os-button primary :label="$t(transferCtx.checkout ? 'specimens.checkout' : 'specimens.checkin')"
        @click="transferSpecimen" />
    </template>
  </os-dialog>

  <os-plugin-views ref="moreMenuPluginViews" page="specimen-detail" view="more-menu" :viewProps="ctx" />
</template>

<script>

import EventsSummaryList from './EventsSummaryList.vue';
import SpecimenTree from '@/biospecimen/components/SpecimenTree.vue';

import specimenSvc from '@/biospecimen/services/Specimen.js';
import wfSvc from '@/biospecimen/services/Workflow.js';

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import util  from '@/common/services/Util.js';

import formSvc from '@/forms/services/Form.js';

import transferSchema from '@/biospecimen/schemas/specimens/transfer.js';


export default {
  props: ['cpr', 'visit'],

  components: {
    EventsSummaryList,
    SpecimenTree
  },

  inject: ['cpViewCtx', 'specimen'],

  data() {
    const cp = this.cpViewCtx.getCp();
    return {
      ctx: {
        cp,

        cpr: this.cpr,

        visit: this.visit,

        specimen: {},

        dict: [],

        auditObjs: [],

        routeQuery: this.$route.query,

        children: [],

        eventForms: undefined,

        userRole: this.cpViewCtx.getRole(),

        moreOptions: []
      },

      pluginViewProps: { },

      transferSchema,

      transferCtx: {
        checkout: false,

        specimen: {}
      }
    };
  },

  async created() {
    this._setupSpecimen();
    this.ctx.dict = await this.cpViewCtx.getSpecimenDict();
    this._loadMoreMenuOptions();
  },

  watch: {
    specimen: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupSpecimen();
        this._loadMoreMenuOptions();
      }
    }
  },

  computed: {
    moreOptions: function() {
      const {specimen} = this.ctx;
      const options = [];

      if (this.isUpdateAllowed) {
        const {reserved, activityStatus, status, storageLocation, checkoutPosition} = specimen;
        if (!reserved && activityStatus == 'Active' && status == 'Collected') {
          options.push({icon: 'times', caption: this.$t('common.buttons.close'), onSelect: this.closeSpecimen});
        } else if (activityStatus == 'Closed') {
          options.push({icon: 'check', caption: this.$t('common.buttons.reopen'), onSelect: this.reopenSpecimen});
        }

        if (!reserved && activityStatus == 'Active' && storageLocation && storageLocation.id > 0) {
          options.push({icon: 'sign-out-alt', caption: this.$t('specimens.checkout'), onSelect: this.checkoutSpecimen});
        }

        if (!reserved && activityStatus == 'Active' && checkoutPosition && checkoutPosition.id > 0) {
          options.push({icon: 'sign-in-alt', caption: this.$t('specimens.checkin'), onSelect: this.checkinSpecimen});
        }
      }

      if (specimen.id > 0 && this.isDeleteAllowed) {
        options.push({icon: 'trash', caption: this.$t('common.buttons.delete'), onSelect: this.deleteSpecimen});
      }

      return options;
    },

    isCreateChildrenAllowed: function() {
      return this.cpViewCtx.isCreateAllSpecimenAllowed(this.cpr);
    },

    isAnyUserUpdateAllowed: function() {
      const vc = this.cpViewCtx;
      const {specimen: {lineage}} = this.ctx;
      return lineage == 'New' ? vc.isUpdateSpecimenAllowed(this.cpr) : vc.isUpdateAllSpecimenAllowed(this.cpr);
    },

    isUpdateAllowed: function() {
      return this.isAnyUserUpdateAllowed && this.notCoordinatOrStoreAllowed;
    },

    isDeleteAllowed: function() {
      const vc = this.cpViewCtx;
      const {specimen: {lineage}} = this.ctx;
      return lineage == 'New' ? vc.isDeleteSpecimenAllowed(this.cpr) : vc.isDeleteAllSpecimenAllowed(this.cpr);
    },

    isPrintAllowed: function() {
      return this.cpViewCtx.isPrintSpecimenAllowed(this.cpr) && this.notCoordinatOrStoreAllowed;
    },

    notCoordinatOrStoreAllowed: function() {
      return this.cpViewCtx.notCoordinatOrStoreAllowed(this.specimen || {});
    }
  },

  methods: {
    edit: function() {
      const {cpId, cprId, visitId, eventId, id} = this.specimen;
      routerSvc.goto('SpecimenAddEdit', {cpId, cprId, visitId, specimenId: id}, {eventId});
    },

    createAliquots: function() {
      wfSvc.createAliquots([this.ctx.specimen]);
    },

    createDerivatives: function() {
      wfSvc.createDerivedSpecimens([this.ctx.specimen]);
    },

    confirmPrint: function() {
      this.$refs.printDialog.open();
    },

    printLabels: function(includeChildren) {
      const {specimen, children} = this.ctx;
      const ids = includeChildren ? this._getChildrenIds(children) : [];
      ids.unshift(specimen.id);

      const ts = util.formatDate(new Date(), 'yyyyMMdd_HHmmss');
      const outputFilename = [
        specimen.cpShortTitle, specimen.ppid,
        specimen.visitName, specimen.label || specimen.id,
        ts
      ].join('_') + '.csv';
      specimenSvc.printLabels({specimenIds: ids}, outputFilename);
      this.$refs.printDialog.close();
    },

    cancelPrint: function() {
      this.$refs.printDialog.close();
    },

    loadEventForms: function() {
      if (this.ctx.eventForms) {
        return;
      }

      const context = {cp: this.cp, cpr: this.cpr, visit: this.visit, specimen: this.specimen};
      this.cpViewCtx.getSpecimenEventForms(context).then(
        eventForms => {
          this.ctx.eventForms = eventForms
            .filter(f => !f.sysForm)
            .map(eventForm => ({caption: eventForm.formCaption, onSelect: () => this.addEvent(eventForm)}));
        }
      );
    },

    addEvent: function(form) {
      const {cpId, cprId, visitId, eventId, id} = this.specimen;
      routerSvc.goto('SpecimenEventAddEdit', {cpId, cprId, visitId, specimenId: id}, {eventId, formId: form.formId, formCtxtId: form.formCtxtId});
    },

    editEvent: function(event) {
      const {cpId, cprId, visitId, eventId, id} = this.specimen;
      const {formId, formCtxtId, id: recordId} = event;
      routerSvc.goto('SpecimenEventAddEdit', {cpId, cprId, visitId, specimenId: id}, {eventId, formId, formCtxtId, recordId});
    },

    showEvent: function(event) {
      const {formId, id: recordId} = event;
      formSvc.getRecord({formId, recordId}, {includeMetadata: true}).then(
        (record) => {
          this.ctx.event = event;
          this.ctx.eventRecord = this.pluginViewProps.record = record;
          this.$refs.eventOverviewDialog.open();
        }
      );
    },

    deleteEvent: function(event) {
      this.ctx.event = event;
      this.$refs.eventDeleteConfirmDialog.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          const {formId, id: recordId} = event;
          formSvc.deleteRecord({formId, recordId}).then(
            () => {
              this.closeEventOverview();
              alertsSvc.success({code: 'specimens.event_deleted', args: {eventName: event.name}});
              this._loadEvents(this.specimen);
            }
          );
        }
      );
    },

    closeEventOverview: function() {
      this.$refs.eventOverviewDialog.close();
      this.ctx.event = this.ctx.eventRecord = null;
    },

    closeSpecimen: function() {
      this.$refs.closeSpmnDialog.open().then(
        (resp) => {
          if (resp) {
            specimenSvc.clearSpecimens(this.visit);
            this.specimen.storageLocation = null;
            this.specimen.initialQty = resp[0].initialQty;
            this.specimen.availableQty = resp[0].availableQty;
            this.specimen.availabilityStatus = this.specimen.activityStatus = 'Closed';
            this._loadEvents(this.specimen);
          }
        }
      );
    },

    reopenSpecimen: function() {
      specimenSvc.saveOrUpdate({id: this.specimen.id, activityStatus: 'Active'}).then(
        (saved) => {
          specimenSvc.clearSpecimens(this.visit);
          this.specimen.activityStatus = 'Active';
          this.specimen.availabilityStatus = saved.availabilityStatus;
        }
      );
    },

    deleteSpecimen: function() {
      this.$refs.deleteSpmnDialog.execute().then(
        (resp) => {
          if (resp != 'deleted') {
            return;
          }

          const route = routerSvc.getCurrentRoute();
          if (route.name.indexOf('ParticipantsListItemSpecimenDetail') >= 0) {
            routerSvc.goto('ParticipantsListItemVisitDetail.Overview', this.specimen);
          } else {
            routerSvc.goto('VisitDetail.Overview', this.specimen);
          }

          specimenSvc.clearSpecimens(this.visit);
        }
      );
    },

    checkoutSpecimen: function() {
      this.transferCtx = {
        checkout: true,
        specimen: {
          id: this.specimen.id,
          storageLocation: null,
          checkout: true,
          transferUser: this.$ui.currentUser,
          transferTime: Date.now()
        }
      };

      this.$refs.transferDialog.open();
    },

    checkinSpecimen: function() {
      this.transferCtx = {
        specimen: {
          id: this.specimen.id,                       //
          specimenClass: this.specimen.specimenClass, // needed for select/load of containers in
          type: this.specimen.type,                   // positions widget
          cpId: this.specimen.cpId,                   //
          storageLocation: {...this.specimen.checkoutPosition},
          transferUser: this.$ui.currentUser,
          transferTime: Date.now()
        }
      };

      this.$refs.transferDialog.open();
    },

    transferSpecimen: function() {
      if (!this.$refs.transferForm.validate()) {
        return;
      }

      specimenSvc.saveOrUpdate(this.transferCtx.specimen).then(
        ({storageLocation, checkedOut, checkoutPosition}) => {
          Object.assign(this.specimen, {storageLocation, checkedOut, checkoutPosition});
          this._loadEvents(this.specimen);
          this.cancelTransfer();
        }
      );
    },

    cancelTransfer: function() {
      this.$refs.transferDialog.close();
    },

    reloadChildren: function() {
      this.ctx.children = [];

      const {specimen} = this.ctx;
      specimenSvc.getById(specimen.id).then(dbSpmn => this.ctx.children = dbSpmn.children);
    },

    _setupSpecimen: function() {
      const specimen = this.ctx.specimen = this.specimen;
      if (specimen.id > 0) {
        this.ctx.auditObjs = [
          {objectId: specimen.id, objectName: 'specimen'}
        ]

        this.ctx.deleteOpts = {
          type: this.$t('specimens.specimen'),
          title: specimen.label + (specimen.barcode ? ' (' + specimen.barcode + ')' : ''),
          dependents: () => specimenSvc.getDependents(specimen),
          forceDelete: true,
          askReason: true,
          deleteObj: (reason) => specimenSvc.deleteSpecimen(specimen.id, true, reason)
        };

        this._loadEvents(specimen);
      }

      this.ctx.children = specimen.children || [];
    },

    _getChildrenIds: function(children) {
      const result = [];
      for (let child of (children || [])) {
        result.push(child.id);
        Array.prototype.push.apply(result, this._getChildrenIds(child.children));
      }

      return result;
    },

    _loadEvents: function(specimen) {
      if (specimen.availabilityStatus && specimen.availabilityStatus != 'Pending') {
        specimenSvc.getEvents(this.specimen).then(events => this.ctx.events = events);
      }
    },

    _loadMoreMenuOptions: function() {
      const options = this.moreOptions || [];
      const ctxt = this.pluginViewProps = {...this.ctx, cpViewCtx: this.cpViewCtx};
      util.getPluginMenuOptions(this.$refs.moreMenuPluginViews, 'specimen-detail', 'more-menu', ctxt)
        .then(pluginOptions => this.ctx.moreOptions = options.concat(pluginOptions));
    }
  }
}
</script>
