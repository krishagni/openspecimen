<template>
  <os-grid v-if="ctx.events && ctx.events.length > 0">
    <os-grid-column :width="3" v-if="!cp.specimenCentric">
      <os-list-group :list="ctx.events" :selected="ctx.selectedEvent" @on-item-select="onEventSelect($event.item)">
        <template #header>
          <span v-t="'cps.events'">Events</span>
        </template>
        <template #actions>
          <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="addEvent"
            v-if="!cp.specimenCentric" />
        </template>
        <template #default="{item: {cpe}}">
          <div class="event">
            <span class="description">
              <os-visit-event-desc :event="cpe" :show-status="true" />
            </span>
            <os-button-group class="actions" @click="onEventOpsClick($event)">
              <os-menu icon="ellipsis-v" :lazy-load="true" :options="eventOps({cpe})" />
            </os-button-group>
          </div>
        </template>
      </os-list-group>
    </os-grid-column>

    <os-grid-column :width="cp.specimenCentric ? 12 : 9">
      <os-panel class="os-full-height-panel">
        <template #header>
          <span v-t="'cps.srs'">Specimen Requirements</span>
        </template>

        <template #actions v-if="ctx.selectedEvent && ctx.selectedEvent.cpe.id > 0 &&
          ctx.selectedEvent.cpe.activityStatus == 'Active'">
          <os-button left-icon="plus" :label="$t('cps.add_req')" @click="addReq(ctx.selectedEvent.cpe)" />
        </template>

        <template #default>
          <os-table-form class="os-cp-reqs-tab" ref="reqsTable" :tree-layout="true" :read-only="true"
            :data="{}" :items="ctx.selectedEvent.cpe.reqs" :expanded="ctx.expandedReqs"
            :schema="reqsListSchema" :show-row-actions="true"
            @row-clicked="onReqClick($event.item)"
            @toggle-node="onToggleNode($event.item)"
            v-if="ctx.selectedEvent && ctx.selectedEvent.cpe.reqs && ctx.selectedEvent.cpe.reqs.length > 0">

            <template #row-actions="{rowItem}" v-show-if-allowed="cpResources.updateOpts">
              <os-menu icon="ellipsis-v" :lazy-load="true" :options="reqOps(ctx.selectedEvent.cpe, rowItem.item)" />
            </template>

            <template #expanded-row>
              <os-overview :schema="reqSchema.fields" :object="reqCtx" />
            </template>
          </os-table-form>

          <div v-else-if="ctx.selectedEvent && ctx.selectedEvent.cpe.reqs && ctx.selectedEvent.cpe.reqs.length == 0">
            <div v-if="ctx.selectedEvent.cpe.activityStatus == 'Active'">
              <os-message type="info">
                <span v-t="'cps.no_reqs_add_new'">No specimen requirements to show. Create a new specimen requirement by click on the Add Requirement button below</span>
              </os-message>

              <os-button left-icon="plus" :label="$t('cps.add_req')" @click="addReq(ctx.selectedEvent.cpe)" />
            </div>

            <div v-else>
              <os-message type="info">
                <span v-t="'cps.no_reqs'">No specimen requirements to show.</span>
              </os-message>
            </div>
          </div>
        </template>
      </os-panel>
    </os-grid-column>
  </os-grid>

  <os-grid v-else>
    <os-grid-column :width="12">
      <div v-if="ctx.events && ctx.events.length == 0">
        <os-message type="info">
          <span v-t="'cps.no_cpes'"></span>
        </os-message>

        <os-button left-icon="plus" :label="$t('cps.add_cpe')" @click="addEvent" />
      </div>

      <div v-else>
        <os-message type="info">
          <span v-t="'common.loading'">Loading...</span>
        </os-message>
      </div>
    </os-grid-column>
  </os-grid>

  <os-confirm ref="confirmDeleteEventDialog">
    <template #title>
      <span v-t="'cps.delete_event_q'">Delete Event?</span>
    </template>
    <template #message>
      <span v-t="{path: 'cps.confirm_delete_event', args: ctx.event}"></span>
    </template>
  </os-confirm>

  <os-confirm ref="confirmCloseEventDialog">
    <template #title>
      <span v-t="'cps.close_event_q'">Close Event?</span>
    </template>
    <template #message>
      <span v-t="{path: 'cps.confirm_close_event', args: ctx.event}"></span>
    </template>
  </os-confirm>

  <os-confirm ref="confirmDeleteReqDialog">
    <template #title>
      <span v-t="'cps.delete_req_q'">Delete Requirement?</span>
    </template>
    <template #message>
      <span v-t="{path: 'cps.confirm_delete_req', args: ctx.req}"></span>
    </template>
  </os-confirm>

  <os-confirm ref="confirmCloseReqDialog">
    <template #title>
      <span v-t="'cps.close_req_q'">Close Requirement?</span>
    </template>
    <template #message>
      <span v-t="{path: 'cps.confirm_close_req', args: ctx.req}"></span>
    </template>
  </os-confirm>
</template>

<script>

import reqsListSchema from '@/biospecimen/schemas/cps/reqs-list.js';
import reqSchema from '@/biospecimen/schemas/cps/req.js';

import alertsSvc  from '@/common/services/Alerts.js';
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc  from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';
import util       from '@/common/services/Util.js';

import cpResources from './Resources.js';

export default {
  props: ['cp', 'event', 'reqId'],

  data() {
    return {
      ctx: {
        events: null,

        selectedEvent: null,

        expandedReqs: []
      },

      cpResources,

      reqSchema
    }
  },

  created() {
    this._loadEvents().then(({reqs}) => this.autoSelectReq(reqs));
    settingSvc.getSetting('biospecimen', 'enable_spmn_barcoding')
      .then(settings => this.ctx.barcodingEnabled = util.isTrue(settings[0].value));
  },

  watch: {
    eventSr: {
      handler: function(newVal, oldVal) {
        const {eventId: oldEventId, reqId: oldReqId} = oldVal || {};
        const {eventId: newEventId, reqId: newReqId} = newVal || {};

        if (newEventId != oldEventId) {
          this.autoSelectEvent().then(reqs => this.autoSelectReq(reqs));
        } else if (newReqId != oldReqId) {
          const eventItem = this.ctx.selectedEvent;
          const {cpe} = eventItem || {cpe: {}};
          this.autoSelectReq(cpe.reqs);
        }
      },

      deep: true
    }
  },

  computed: {
    eventSr: function() {
      return {eventId: this.event.id, reqId: this.reqId};
    },

    reqsListSchema: function() {
      const result = util.clone(reqsListSchema);
      for (const field of result.columns) {
        if (field.name == 'sr' && field.type == 'specimen-description') {
          field.showStatus = true;
          field.noLink = true;
        }
      }

      return result;
    },

    reqCtx: function() {
      const barcodingEnabled = util.isTrue(this.ctx.barcodingEnabled || this.cp.barcodingEnabled);
      if (this.ctx.selectedEvent) {
        return {cp: this.cp, cpe: this.ctx.selectedEvent.cpe, sr: this.ctx.expandedReqs[0].sr, barcodingEnabled};
      }

      return {barcodingEnabled};
    },

    hasReqs: function() {
      const {events} = this.ctx;
      const {cpe} = events && events.length > 0 ? events[0] : {};
      const reqs = (cpe && cpe.reqs) || [];
      return reqs.length > 0;
    }
  },

  methods: {
    autoSelectEvent: async function() {
      let event = null;
      if (this.event.id > 0 ) {
        event = this.ctx.events.find(({cpe}) => cpe.id == this.event.id);
        if (event == null) {
          routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {});
          return;
        }

        this.onEventSelect(event, true);
      } else {
        event = this.ctx.events.length > 0 ? this.ctx.events[0] : null;
        if (event && event.cpe && event.cpe.id > 0) {
          this.onEventSelect(event, false);
          return;
        }
      }

      const prev = this.ctx.selectedEvent;
      this.ctx.selectedEvent = null;

      let reqs = null;
      if (event && (!prev || prev != event)) {
        this.ctx.selectedEvent = event;

        const {cpe} = event;
        reqs = cpe.reqs;
        if (!reqs) {
          reqs = await this._loadReqs(cpe);
        }
      }

      return reqs;
    },

    autoSelectReq: function(reqs) {
      let req = null;
      if (this.reqId > 0 && reqs) {
        req = reqs.find(({sr}) => sr.id == this.reqId);
        if (req == null) {
          alert('Invalid SR ID');
          routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: this.event.id});
          return;
        }

        if (!this.ctx.expandedReqs || this.ctx.expandedReqs.length == 0 || this.ctx.expandedReqs[0] != req) {
          this.onReqClick(req);
          req.show = true;
          setTimeout(
            () => {
              let parent = req.parent;
              while (parent) {
                parent.show = parent.expanded = true;
                if (parent) {
                  for (let sibling of reqs) {
                    if (sibling.parentUid == parent.uid) {
                      sibling.show = true;
                    }
                  }
                }
                parent = parent.parent;
              }

              this.$refs.reqsTable.scrollExpandedIntoView();
            }
          );
        }
      }
    },

    onEventSelect: function({cpe: {id: eventId}}, auto) {
      if (auto) {
        return;
      }

      routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, eventId == this.event.id ? {} : {eventId});
    },

    onEventOpsClick: function(event) {
      event.stopPropagation();
    },

    eventOps: function({cpe}) {
      const ops = [];
      if (cpe.activityStatus != 'Closed') {
        ops.push({icon: 'edit',  caption: this.$t('common.buttons.edit'),   onSelect: () => this.editEvent(cpe)});
        ops.push({icon: 'copy',  caption: this.$t('common.buttons.copy'),   onSelect: () => this.copyEvent(cpe)});
      }

      ops.push({icon: 'trash', caption: this.$t('common.buttons.delete'), onSelect: () => this.deleteEvent(cpe)});
      if (cpe.activityStatus != 'Closed') {
        ops.push({icon: 'ban',   caption: this.$t('common.buttons.close'),  onSelect: () => this.closeEvent(cpe)});
        ops.push({divider: true});
        ops.push({icon: 'flask', caption: this.$t('cps.add_req'),  onSelect: () => this.addReq(cpe)});
      } else {
        ops.push({icon: 'check', caption: this.$t('common.buttons.reopen'), onSelect: () => this.reopenEvent(cpe)});
      }

      return ops;
    },

    addEvent: function() {
      routerSvc.goto('CpDetail.Events.AddEdit', {cpId: this.cp.id}, {eventId: -1});
    },

    editEvent: function(cpe) {
      routerSvc.goto('CpDetail.Events.AddEdit', {cpId: this.cp.id}, {eventId: cpe.id});
    },

    copyEvent: function(cpe) {
      routerSvc.goto('CpDetail.Events.AddEdit', {cpId: this.cp.id}, {eventId: -1, copyOf: cpe.id});
    },

    deleteEvent: async function(cpe) {
      this.ctx.event = cpe;
      const resp = await this.$refs.confirmDeleteEventDialog.open();
      if (resp == 'proceed') {
        cpSvc.deleteCpe(cpe).then(
          () => {
            alertsSvc.success({code: 'cps.event_deleted', args: cpe});
            this.ctx.selectedEvent = null;
            this._loadEvents();
          }
        );
      }
    },

    closeEvent: async function(cpe) {
      this.ctx.event = cpe;
      const resp = await this.$refs.confirmCloseEventDialog.open();
      if (resp == 'proceed') {
        const toSave = {...cpe};
        toSave.activityStatus = 'Closed';
        delete toSave.reqs;

        cpSvc.saveOrUpdateCpe(toSave).then(
          () => {
            alertsSvc.success({code: 'cps.event_closed', args: cpe});
            this.ctx.selectedEvent = null;
            this._loadEvents();
          }
        );
      }
    },

    reopenEvent: async function(cpe) {
      const toSave = {...cpe};
      toSave.activityStatus = 'Active';
      delete toSave.reqs;

      cpSvc.saveOrUpdateCpe(toSave).then(
        () => {
          alertsSvc.success({code: 'cps.event_reopened', args: cpe});
          this.ctx.selectedEvent = null;
          this._loadEvents();
        }
      );
    },

    onReqClick: function(item) {
      const [prev] = this.ctx.expandedReqs;
      this.ctx.expandedReqs.length = 0;
      if (!prev || prev != item) {
        this.ctx.expandedReqs = [item];
        setTimeout(() => this.$refs.reqsTable.scrollExpandedIntoView());
        routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: this.event.id, reqId: item.sr.id});
      } else {
        routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: this.event.id});
      }
    },

    onToggleNode: function(item) {
      const [prev] = this.ctx.expandedReqs;
      if (prev == item) {
        this.ctx.expandedReqs.length = 0;
        routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: this.event.id});
      }
    },

    reqOps: function(cpe, {sr}) {
      const ops = [];
      if (sr.activityStatus == 'Active') {
        ops.push({icon: 'edit', caption: this.$t('common.buttons.edit'), onSelect: () => this.editReq(sr)});
        ops.push({icon: 'copy', caption: this.$t('common.buttons.copy'), onSelect: () => this.copyReq(cpe, sr)});
        ops.push({divider: true});
        ops.push({icon: 'share-alt', caption: this.$t('cps.create_aliquots'), onSelect: () => this.createAliquots(cpe, sr)});
        ops.push({icon: 'flask', caption: this.$t('cps.create_derivative'), onSelect: () => this.createDerivative(cpe, sr)});
        ops.push({divider: true});
        ops.push({icon: 'ban', caption: this.$t('common.buttons.close'), onSelect: () => this.closeReq(cpe, sr)});
      } else {
        ops.push({icon: 'check', caption: this.$t('common.buttons.reopen'), onSelect: () => this.reopenReq(cpe, sr)});
      }

      ops.push({icon: 'trash', caption: this.$t('common.buttons.delete'), onSelect: () => this.deleteReq(cpe, sr)});
      return ops;
    },

    addReq: function(cpe) {
      routerSvc.goto('CpDetail.Events.AddEditReq', {cpId: this.cp.id}, {eventId: cpe.id});
    },

    editReq: function(sr) {
      routerSvc.goto('CpDetail.Events.AddEditReq', {cpId: this.cp.id}, {eventId: sr.eventId, reqId: sr.id});
    },

    copyReq: function(cpe, sr) {
      cpSvc.copyRequirement(sr.id).then(
        savedReq => {
          alertsSvc.success({code: 'cps.req_copy_created', args: sr});
          this._loadReqs(cpe).then(
            () => {
              routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: sr.eventId, reqId: savedReq.id});
            }
          );
        }
      );
    },

    createAliquots: function(cpe, sr) {
      routerSvc.goto('CpDetail.Events.CreateAliquots', {cpId: this.cp.id}, {eventId: cpe.id, parentReqId: sr.id});
    },

    createDerivative: function(cpe, sr) {
      routerSvc.goto('CpDetail.Events.CreateDerivedReq', {cpId: this.cp.id}, {eventId: cpe.id, parentReqId: sr.id});
    },

    deleteReq: async function(cpe, sr) {
      this.ctx.req = sr;
      const resp = await this.$refs.confirmDeleteReqDialog.open();
      if (resp != 'proceed') {
        return;
      }

      cpSvc.deleteRequirement(sr).then(
        () => {
          alertsSvc.success({code: 'cps.req_deleted', args: sr});
          this._loadReqs(cpe).then(
            () => {
              routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: sr.eventId});
            }
          );
        }
      );
    },

    closeReq: async function(cpe, sr) {
      this.ctx.req = sr;
      const resp = await this.$refs.confirmCloseReqDialog.open();
      if (resp != 'proceed') {
        return;
      }

      const toSave = util.clone(sr);
      toSave.children = null;
      toSave.activityStatus = 'Closed';
      cpSvc.saveOrUpdateSpecimenRequirement(toSave).then(
        () => {
          alertsSvc.success({code: 'cps.req_closed', args: sr});
          this._loadReqs(cpe).then(
            () => {
              routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: sr.eventId, reqId: sr.id});
            }
          );
        }
      );
    },

    reopenReq: async function(cpe, sr) {
      const toSave = util.clone(sr);
      toSave.children = null;
      toSave.activityStatus = 'Active';
      cpSvc.saveOrUpdateSpecimenRequirement(toSave).then(
        () => {
          this._loadReqs(cpe).then(
            () => {
              routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: sr.eventId, reqId: sr.id});
            }
          );
        }
      );
    },

    _loadEvents: function() {
      return cpSvc.getCpes(this.cp.id).then(
        cpes => {
          const events = this.ctx.events = cpes.map(cpe => ({cpe}));
          if (events.length == 0 && this.cp.specimenCentric) {
            events.push({cpe: {reqs: [], activityStatus: 'Active'}});
          }

          return this.autoSelectEvent().then(reqs => ({events, reqs}));
        }
      );
    },

    _loadReqs: async function(cpe) {
      const reqs = await cpSvc.getSpecimenRequirements(this.cp.id, cpe.id, true);
      cpe.reqs = this._flattenReqs(cpe, reqs, 0);
      return cpe.reqs;
    },

    _flattenReqs: function(cpe, reqs, depth, parent) {
      let result = [], idx = 0;
      for (let req of reqs) {
        ++idx;

        const parentUid = parent && parent.uid;
        const uid = parentUid !== undefined && parentUid !== null ? (parentUid + '_' + idx) : ('' + idx);
        const item = {cp: this.cp, event: cpe, sr: req, depth, expanded: false, show: !parentUid, parentUid, uid, parent};
        result.push(item);

        req.status = req.activityStatus == 'Closed' ? 'Closed' : 'Pending';
        if (req.defaultCustomFieldValues) {
          req.defaultCustomFieldValuesJson = JSON.stringify(req.defaultCustomFieldValues, null, 2);
        }

        const children = this._flattenReqs(cpe, req.children || [], depth + 1, item);
        Array.prototype.push.apply(result, children);
        item.hasChildren = (children || []).length > 0;
      }

      return result;
    }
  }
}
</script>

<style scoped>
.event {
  display: flex;
  align-items: center;
}

.event .description {
  flex: 1;
}

.os-cp-reqs-tab {
  height: 100%;
  overflow-y: auto;
}
</style>
