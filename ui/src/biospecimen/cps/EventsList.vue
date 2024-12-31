<template>
  <os-page-toolbar>
    <template #default v-show-if-allowed="cpResources.updateOpts">
      <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="addEvent"
        v-if="!cp.specimenCentric" />

      <os-button left-icon="plus" :label="$t('cps.add_req')" @click="addReq(ctx.events[0].cpe)" v-else-if="hasReqs" />
    </template>
  </os-page-toolbar>

  <div class="os-cp-events-tab">
    <os-list-view class="os-muted-list-header"
      :class="cp.specimenCentric ? ['os-cp-hide-events'] : []"
      :data="ctx.events"
      :schema="eventsListSchema"
      :expanded="ctx.expandedEvents"
      :showRowActions="true"
      @rowClicked="onEventRowClick"
      ref="listView">

      <template #rowActions="{rowObject}" v-show-if-allowed="cpResources.updateOpts">
        <os-button-group>
          <os-menu icon="ellipsis-v" :lazy-load="true" :options="eventOps(rowObject)" />
        </os-button-group>
      </template>

      <template class="event-details" #expansionRow="{rowObject: {cpe}}">
        <os-table-form class="os-cp-reqs-tab" ref="reqsTable" :tree-layout="true" :read-only="true"
          :data="{}" :items="cpe.reqs" :expanded="ctx.expandedReqs"
          :schema="reqsListSchema" :show-row-actions="true"
          @row-clicked="onReqClick($event.item)" v-if="cpe.reqs && cpe.reqs.length > 0">

          <template #row-actions="{rowItem}" v-show-if-allowed="cpResources.updateOpts">
            <os-menu icon="ellipsis-v" :lazy-load="true" :options="reqOps(cpe, rowItem.item)" />
          </template>

          <template #expanded-row>
            <os-overview :schema="reqSchema.fields" :object="reqCtx" />
          </template>
        </os-table-form>

        <div v-else-if="cpe.reqs && cpe.reqs.length == 0">
          <div v-if="cpe.activityStatus == 'Active'">
            <os-message type="info">
              <span v-t="'cps.no_reqs_add_new'">No specimen requirements to show. Create a new specimen requirement by click on the Add Requirement button below</span>
            </os-message>

            <os-button left-icon="plus" :label="$t('cps.add_req')" @click="addReq(cpe)" />
          </div>

          <div v-else>
            <os-message type="info">
              <span v-t="'cps.no_reqs'">No specimen requirements to show.</span>
            </os-message>
          </div>
        </div>
      </template>
    </os-list-view>
  </div>

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

import eventsListSchema from '@/biospecimen/schemas/cps/events-list.js';
import reqsListSchema from '@/biospecimen/schemas/cps/reqs-list.js';
import reqSchema from '@/biospecimen/schemas/cps/req.js';

import alertsSvc  from '@/common/services/Alerts.js';
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc  from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';
import util       from '@/common/services/Util.js';

import cpResources from './Resources.js';

export default {
  props: ['cp', 'eventId', 'reqId'],

  data() {
    return {
      ctx: {
        events: [],

        expandedEvents: [],

        expandedReqs: []
      },

      cpResources,

      eventsListSchema,

      reqSchema
    }
  },

  created() {
    this._loadEvents().then(() => this.autoSelectEvent().then(reqs => this.autoSelectReq(reqs)));

    settingSvc.getSetting('biospecimen', 'enable_spmn_barcoding')
      .then(settings => this.ctx.barcodingEnabled = util.isTrue(settings[0].value));
  },

  watch: {
    eventSr: {
      handler: function(newVal, oldVal) {
        const {eventId: oldEventId, reqId: oldReqId} = oldVal || {};
        const {eventId: newEventId, reqId: newReqId} = newVal || {};

        if (newEventId != oldEventId) {
          this.autoSelectEvent().then(
            (reqs) => {
              this.autoSelectReq(reqs);
            }
          );
        } else if (newReqId != oldReqId) {
          const [eventItem] = this.ctx.expandedEvents;
          const {cpe} = eventItem || {cpe: {}};
          this.autoSelectReq(cpe.reqs);
        }
      },

      deep: true
    }
  },

  computed: {
    eventSr: function() {
      return {eventId: this.eventId, reqId: this.reqId};
    },

    reqsListSchema: function() {
      const result = util.clone(reqsListSchema);
      for (let field of result.columns) {
        if (field.name == 'sr' && field.type == 'specimen-description') {
          field.showStatus = true;
          field.noLink = true;
        }
      }

      return result;
    },

    reqCtx: function() {
      const barcodingEnabled = util.isTrue(this.ctx.barcodingEnabled || this.cp.barcodingEnabled);
      if (this.ctx.expandedReqs.length > 0) {
        return {cp: this.cp, cpe: this.ctx.expandedEvents[0].cpe, sr: this.ctx.expandedReqs[0].sr, barcodingEnabled};
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
      if (this.eventId > 0 ) {
        event = this.ctx.events.find(({cpe}) => cpe.id == this.eventId);
        if (event == null) {
          alert('Invalid event ID');
          routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {});
          return;
        }

        this.onEventRowClick(event, true);
      } else if (this.cp.specimenCentric) {
        event = this.ctx.events.length > 0 ? this.ctx.events[0] : null;
        if (event && event.cpe && event.cpe.id > 0) {
          this.onEventRowClick(event, false);
          return;
        }
      }

      const [prev] = this.ctx.expandedEvents;
      this.ctx.expandedEvents.length = 0;

      let reqs = null;
      if (event && (!prev || prev != event)) {
        this.ctx.expandedEvents = [event];

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
          routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: this.eventId});
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

    onEventRowClick: function({cpe: {id: eventId}}, auto) {
      if (auto) {
        return;
      }

      routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, eventId == this.eventId ? {} : {eventId});
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
          this._loadEvents();
        }
      );
    },

    onReqClick: function(item) {
      const [prev] = this.ctx.expandedReqs;
      this.ctx.expandedReqs.length = 0;
      if (!prev || prev != item) {
        this.ctx.expandedReqs = [item];
        routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: this.eventId, reqId: item.sr.id});
      } else {
        routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {eventId: this.eventId});
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

          return this.ctx.events;
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
.os-cp-events-tab :deep(.os-list .results .results-inner) {
  padding-right: 0rem;
}

.os-cp-events-tab :deep(table.p-datatable-table) {
  border: 0px;
  border-collapse: separate;
  border-spacing: 2px 15px;
  margin-top: -0.5rem;
  margin-bottom: 0rem;
  table-layout: fixed;
}

.os-cp-events-tab :deep(table.p-datatable-table > thead > tr > th) {
  background: transparent!important;
  border-bottom: 0;
  padding: 0rem 1rem;
  text-align: center;
}

.os-cp-events-tab :deep(table.p-datatable-table > thead > tr > th.row-actions) {
  width: 90px;
}

.os-cp-events-tab :deep(table.p-datatable-table > tbody > tr > td) {
  border-top: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.os-cp-events-tab :deep(table.p-datatable-table > tbody > tr) {
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 2px 6px 2px rgba(60,64,67,.15);
  border-radius: 0.5rem;
  background: transparent;
}

.os-cp-events-tab :deep(table.p-datatable-table > tbody > tr:hover) {
  background: transparent;
}

.os-cp-events-tab :deep(table.p-datatable-table > tbody > tr > td) {
  vertical-align: middle;
  padding: 1rem;
}

.os-cp-reqs-tab {
  max-height: 55vh;
  overflow-y: auto;
}

.os-cp-hide-events .os-cp-reqs-tab {
  max-height: calc(100vh - 230px);
}

.os-cp-hide-events :deep(table.p-datatable-table > thead),
.os-cp-hide-events :deep(table.p-datatable-table > tbody > tr:not(.p-datatable-row-expansion)) {
  display: none;
}
</style>
