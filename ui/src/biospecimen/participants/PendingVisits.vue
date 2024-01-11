<template>
  <div class="os-pending-visits-tab">
    <os-list-view
      class="os-muted-list-header os-bordered-list"
      :data="pendingVisits"
      :schema="{columns: tabFields}"
      :showRowActions="true"
      @rowClicked="onVisitRowClick"
      ref="listView">

      <template #rowActions="slotProps">
        <os-button size="small" :label="$t('participants.collect')" @click="collectVisit(slotProps.rowObject)" />
      </template>
    </os-list-view>
  </div>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';

export default {
  props: ['cpr', 'visits'],

  inject: ['cpViewCtx'],

  data() {
    return {
      tabFields: []
    }
  },

  created() {
    this.cpViewCtx.getPendingVisitsTabFields().then(tabFields => this.tabFields = tabFields);
  },

  computed: {
    cpId: function() {
      return this.cpViewCtx.cpId;
    },

    pendingVisits: function() {
      return (this.visits || []).filter(visit => !visit.status || visit.status == 'Pending')
        .map(visit => ({
          visit: Object.assign(
            visit, {cprId: this.cpr.id, anticipatedVisitDate: this._getAnticipatedVisitDate(visit)})
        }));
    }
  },

  methods: {
    collectVisit: async function(rowObject) {
      const wfInstanceSvc = this.$osSvc.tmWfInstanceSvc;
      const {visit} = rowObject;
      if (wfInstanceSvc) {
        let wfName = await this._getCollectVisitsWf();
        if (!wfName) {
          wfName = 'sys-collect-visits';
        }

        let inputItem = {cpr: {id: visit.cprId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle}};
        if (visit.eventId) {
          inputItem.cpe = {id: visit.eventId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle};
        }

        let description = visit.description || 'Unknown';
        let dateIdx = description.indexOf(' / ');
        if (dateIdx >= 0) {
          description = visit.description.substring(0, dateIdx);
        }

        const opts = {
          params: {
            returnOnExit: 'current_view',
            cpId: visit.cpId,
            'breadcrumb-1': JSON.stringify({
              label: visit.cpShortTitle,
              route: {name: 'ParticipantsList', params: {cpId: visit.cpId, cprId: -1}}
            }),
            'breadcrumb-2': JSON.stringify({
              label: visit.ppid,
              route: {name: 'ParticipantsListItemDetail.Overview', params: {cpId: visit.cpId, cprId: visit.cprId}}
            }),
            batchTitle: description,
            showOptions: false
          }
        }

        if (visit.id > 0) {
          opts.inputType = 'visit';
          inputItem.visit = {id: visit.id, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle};
        }

        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
        wfInstanceSvc.gotoInstance(instance.id);
      } else {
        alert('Workflow module not installed!');
      }
    },

    _getCollectVisitsWf() {
      return cpSvc.getWorkflowProperty(this.cpId, 'common', 'collectVisitsWf');
    },

    _getAnticipatedVisitDate(visit) {
      return visit.visitDate || visit.anticipatedVisitDate;
    }
  }
}
</script>

<style scoped>
.os-pending-visits-tab :deep(.os-list .results .results-inner) {
  padding-right: 0rem;
}
</style>
