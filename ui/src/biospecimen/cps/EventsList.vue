<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="addEvent"
        v-show-if-allowed="cpResources.updateOpts" />
    </template>
  </os-page-toolbar>

  <div class="os-cp-events-tab">
    <os-list-view class="os-muted-list-header"
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
        <os-table-form ref="reqsTable" :tree-layout="true" :read-only="true"
          :data="{}" :items="cpe.reqs" :expanded="ctx.expandedReqs"
          :schema="reqsListSchema" :show-row-actions="true"
          @row-clicked="onReqClick($event.item)" v-if="cpe.reqs && cpe.reqs.length > 0">

          <template #row-actions="{rowItem}" v-show-if-allowed="cpResources.updateOpts">
            <os-menu icon="ellipsis-v" :lazy-load="true" :options="reqOps(rowItem.item)" />
          </template>

          <template #expanded-row>
            <os-overview :schema="reqSchema.fields" :object="reqCtx" />
          </template>
        </os-table-form>
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
</template>

<script>

import eventsListSchema from '@/biospecimen/schemas/cps/events-list.js';
import reqsListSchema from '@/biospecimen/schemas/cps/reqs-list.js';
import reqSchema from '@/biospecimen/schemas/cps/req.js';

import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

import cpResources from './Resources.js';

export default {
  props: ['cp', 'eventId'],

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
    this._loadEvents().then(() => this.autoSelectEvent());
  },

  watch: {
    eventId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.autoSelectEvent();
      }
    }
  },

  computed: {
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
      if (this.ctx.expandedReqs.length > 0) {
        return {cp: this.cp, cpe: this.ctx.expandedEvents[0].cpe, sr: this.ctx.expandedReqs[0].sr};
      }

      return {};
    }
  },

  methods: {
    autoSelectEvent: function() {
      let event = null;
      if (this.eventId > 0 ) {
        event = this.ctx.events.find(({cpe}) => cpe.id == this.eventId);
        if (event == null) {
          alert('Invalid event ID');
          routerSvc.goto('CpDetail.Events.List', {cpId: this.cp.id}, {});
          return;
        }

        this.onEventRowClick(event, true);
      }

      const [prev] = this.ctx.expandedEvents;
      this.ctx.expandedEvents.length = 0;
      if (event && (!prev || prev != event)) {
        this.ctx.expandedEvents = [event];

        const {cpe} = event;
        if (!cpe.reqs) {
          cpSvc.getSpecimenRequirements(this.cp.id, cpe.id, true).then(
            reqs => cpe.reqs = this._flattenReqs(cpe, reqs, 0)
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
      return [
        {icon: 'edit',  caption: this.$t('common.buttons.edit'),   onSelect: () => this.editEvent(cpe)},
        {icon: 'copy',  caption: this.$t('common.buttons.copy'),   onSelect: () => this.copyEvent(cpe)},
        {icon: 'trash', caption: this.$t('common.buttons.delete'), onSelect: () => this.deleteEvent(cpe)},
        {icon: 'ban',   caption: this.$t('common.buttons.close'),  onSelect: () => this.closeEvent(cpe)}
      ];
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
        const toSave = util.clone(cpe);
        toSave.activityStatus = 'Closed';
        cpSvc.saveOrUpdateCpe(toSave).then(
          () => {
            alertsSvc.success({code: 'cps.event_closed', args: cpe});
            this._loadEvents();
          }
        );
      }
    },

    reqOps: function({sr}) {
      return [
        {icon: 'edit',  caption: this.$t('common.buttons.edit'),   onSelect: () => this.editSr(sr)},
        {icon: 'copy',  caption: this.$t('common.buttons.copy'),   onSelect: () => this.copySr(sr)},
        {divider: true},
        {icon: 'share-alt',  caption: this.$t('cps.create_aliquots'),   onSelect: () => this.createAliquots(sr)},
        {icon: 'flask',  caption: this.$t('cps.create_derivative'),   onSelect: () => this.createDerivative(sr)},
        {divider: true},
        {icon: 'trash', caption: this.$t('common.buttons.delete'), onSelect: () => this.deleteSr(sr)},
        {icon: 'ban',   caption: this.$t('common.buttons.close'),  onSelect: () => this.closeSr(sr)}
      ];
    },

    onReqClick: function(item) {
      const [prev] = this.ctx.expandedReqs;
      this.ctx.expandedReqs.length = 0;
      if (!prev || prev != item) {
        this.ctx.expandedReqs = [item];
      }
    },

    _loadEvents: function() {
      return cpSvc.getCpes(this.cp.id).then(
        cpes => {
          this.ctx.events = cpes.map(cpe => ({cpe}));
          return this.ctx.events;
        }
      );
    },

    _flattenReqs: function(cpe, reqs, depth) {
      let result = [];
      for (let req of reqs) {
        const item = {cp: this.cp, event: cpe, sr: req, depth, expanded: true, show: true};
        result.push(item);

        const children = this._flattenReqs(cpe, req.children || [], depth + 1);
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
</style>
