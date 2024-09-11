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
      @rowClicked="onRowClick"
      ref="listView">

      <template #rowActions="{rowObject}" v-show-if-allowed="cpResources.updateOpts">
        <os-button-group>
          <os-menu icon="ellipsis-v" :lazy-load="true" :options="eventOps(rowObject)" />
        </os-button-group>
      </template>

      <template class="event-details" #expansionRow="{rowObject}">
        <pre>Show specimen requirements of {{rowObject.cpe.eventLabel}}</pre>
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

import alertsSvc from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

import cpResources from './Resources.js';

export default {
  props: ['cp'],

  data() {
    return {
      ctx: {
        events: [],

        expandedEvents: []
      },

      cpResources,

      eventsListSchema
    }
  },

  created() {
    this._loadEvents();
  },

  methods: {
    onRowClick: function(rowObject) {
      const [prev] = this.ctx.expandedEvents;
      this.ctx.expandedEvents.length = 0;
      if (!prev || prev != rowObject) {
        this.ctx.expandedEvents = [rowObject];
      }
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
      routerSvc.goto('CpsListItemDetail.Events.AddEdit', {cpId: this.cp.id}, {eventId: -1});
    },

    editEvent: function(cpe) {
      routerSvc.goto('CpsListItemDetail.Events.AddEdit', {cpId: this.cp.id}, {eventId: cpe.id});
    },

    copyEvent: function(cpe) {
      routerSvc.goto('CpsListItemDetail.Events.AddEdit', {cpId: this.cp.id}, {eventId: -1, copyOf: cpe.id});
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

    _loadEvents: function() {
      cpSvc.getCpes(this.cp.id).then(cpes => this.ctx.events = cpes.map(cpe => ({cpe})));
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
