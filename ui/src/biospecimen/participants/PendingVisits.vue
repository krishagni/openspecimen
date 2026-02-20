<template>
  <div class="os-pending-visits-tab">
    <div class="action-buttons">
      <os-button :label="$t('participants.collect')" @click="collectVisits"
        v-if="selectedVisits.length > 0 && isCreateOrUpdateVisitAllowed" />
    </div>

    <os-list-view
      class="os-muted-list-header"
      :allow-selection="allowSelection"
      :data="pendingVisits"
      :schema="{columns: tabFields}"
      :showRowActions="true"
      :expanded="expandedVisits"
      @rowClicked="onVisitRowClick"
      @selectedRows="onVisitRowsSelection"
      ref="listView">

      <template #rowActions="slotProps">
        <os-button size="small" :label="$t('participants.collect')" @click="collectVisit(slotProps.rowObject)"
          v-if="cpr.hasConsented && selectedVisits.length == 0 && isCreateOrUpdateVisitAllowed" />
      </template>

      <template class="visit-details" #expansionRow="{rowObject}">
        <os-overview :schema="dict" :object="{cp, cpr, visit: rowObject.visit, userRole}"
          v-if="dict && dict.length > 0" />
      </template>
    </os-list-view>
  </div>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cp', 'cpr', 'visits', 'dict'],

  inject: ['cpViewCtx'],

  data() {
    return {
      tabFields: [],

      selectedVisits: [],

      expandedVisits: [],

      allowSelection: false,

      userRole: null
    }
  },

  created() {
    this.cpViewCtx.getPendingVisitsTabFields().then(tabFields => this.tabFields = tabFields);
    this.cpViewCtx.isMultiVisitsCollectionAllowed().then(allowSelection => this.allowSelection = allowSelection);
    this.userRole = this.cpViewCtx.getRole();
  },

  computed: {
    cpId: function() {
      return this.cpViewCtx.cpId;
    },

    pendingVisits: function() {
      return (this.visits || []).filter(visit => !visit.status || visit.status == 'Pending')
        .map(visit => ({
          visit: Object.assign(
            visit,
            {cprId: this.cpr.id, anticipatedVisitDate: this._getAnticipatedVisitDate(visit)}
          ),
          userRole: this.userRole
        }));
    },

    isCreateOrUpdateVisitAllowed: function() {
      return this.cpViewCtx.isCreateOrUpdateVisitAllowed(this.cpr);
    }
  },

  methods: {
    onVisitRowClick: function(rowObject) {
      const [prev] = this.expandedVisits;
      this.expandedVisits.length = 0;
      if (!prev || prev.visit != rowObject.visit) {
        this.expandedVisits.push(rowObject);
      }
      // this._gotoVisit(visit);
    },

    onVisitRowsSelection: function(event) {
      this.selectedVisits = event.map(({rowObject: {visit}}) => visit);
    },

    collectVisit: function({visit}) {
      this._collectVisits([visit]);
    },

    collectVisits: function() {
      this._collectVisits(this.selectedVisits);
    },

    _collectVisits: async function(visits) {
      let somePrecreated = visits.some(visit => visit.id > 0);
      let someNotCreated = visits.some(visit => !visit.id || visit.id == -1);
      if (somePrecreated && someNotCreated) {
        alertsSvc.error({code: 'participants.all_or_none_visits_precreated'});
        return;
      }

      const wfInstanceSvc = this.$osSvc.tmWfInstanceSvc;
      if (wfInstanceSvc) {
        let wfName = await this._getCollectVisitsWf();
        if (!wfName) {
          wfName = 'sys-collect-visits';
        }

        const opts = {
          params: {
            returnOnExit: 'current_view',
            cpId: this.cp.id,
            cprId: this.cpr.id,
            'breadcrumb-1': JSON.stringify({
              label: this.cp.shortTitle,
              route: {name: 'ParticipantsList', params: {cpId: this.cp.id, cprId: -1}}
            }),
            'breadcrumb-2': JSON.stringify({
              label: this.cpr.ppid,
              route: {name: 'ParticipantsListItemDetail.Overview', params: {cpId: this.cp.id, cprId: this.cpr.id}}
            }),
            showOptions: false,
            collectVisits: true
          }
        }

        const inputItems = visits.map(
          (visit) => {
            let item = {cpr: {id: visit.cprId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle}};
            if (visit.eventId) {
              item.cpe = {id: visit.eventId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle};
            }

            let description = visit.description || 'Unknown';
            let dateIdx = description.indexOf(' / ');
            if (dateIdx >= 0) {
              description = visit.description.substring(0, dateIdx);
            }

            if (opts.params.batchTitle) {
              opts.params.batchTitle = 'Collect Visits';
            } else {
              opts.params.batchTitle = description;
            }

            if (visit.id > 0) {
              opts.inputType = 'visit';
              item.visit = {id: visit.id, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle};
            }

            return item;
          }
        );

        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, inputItems, opts);
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
    },

    _gotoVisit: function({cpId, cprId, id, eventId}) {
      const route = routerSvc.getCurrentRoute();
      const params = {cpId, cprId, visitId: id || -1};
      if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
        routerSvc.goto('ParticipantsListItemVisitDetail.Overview', params, {eventId});
      } else {
        routerSvc.goto('VisitDetail.Overview', params, {eventId});
      }
    }
  }
}
</script>

<style scoped>
.os-pending-visits-tab :deep(table.p-datatable-table) {
  border: 0px;
  border-collapse: separate;
  border-spacing: 2px 15px;
  margin-top: -0.5rem;
  margin-bottom: 0rem;
  table-layout: fixed;
}

.os-pending-visits-tab :deep(.os-key-values .item) {
  border-collapse: collapse;
  border-spacing: 0px;
}

.os-pending-visits-tab :deep(table.p-datatable-table > thead > tr > th) {
  background: transparent!important;
  border-bottom: 0;
  padding: 0rem 1rem;
  text-align: center;
}

.os-pending-visits-tab :deep(table.p-datatable-table > thead > tr > th.os-selection-cb) {
  width: 2.5rem;
}

.os-pending-visits-tab :deep(table.p-datatable-table > thead > tr > th.row-actions) {
  width: 75px;
}

.os-pending-visits-tab :deep(table.p-datatable-table > tbody > tr > td) {
  border-top: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.os-pending-visits-tab :deep(table.p-datatable-table > tbody > tr){
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 2px 6px 2px rgba(60,64,67,.15);
  border-radius: 0.5rem;
  background: transparent;
}

.os-pending-visits-tab :deep(table.p-datatable-table > tbody > tr:hover) {
  background: transparent;
}

.os-pending-visits-tab :deep(table.p-datatable-table > tbody > tr > td) {
  vertical-align: middle;
  padding: 1rem;
}

.os-pending-visits-tab :deep(table.p-datatable-table > tbody > tr > td.os-selection-cb.p-selection-column) {
}

.os-pending-visits-tab .action-buttons {
  margin-bottom: 1.25rem;
}

</style>
