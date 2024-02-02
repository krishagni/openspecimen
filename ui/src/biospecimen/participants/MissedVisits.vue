<template>
  <div class="os-missed-visits-tab">
    <os-list-view
      class="os-muted-list-header os-bordered-list1"
      :data="missedVisits"
      :schema="{columns: tabFields}"
      :showRowActions="true"
      @rowClicked="onVisitRowClick"
      ref="listView">
      <template #rowActions="{rowObject}">
        <os-button-group>
          <os-menu icon="ellipsis-v" :no-outline="true" :options="options(rowObject.visit)" />
        </os-button-group>
      </template>
    </os-list-view>
  </div>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['visits'],

  inject: ['cpViewCtx'],

  data() {
    return {
      tabFields: []
    }
  },

  created() {
    this.cpViewCtx.getMissedVisitsTabFields().then(tabFields => this.tabFields = tabFields);
  },

  computed: {
    missedVisits: function() {
      return (this.visits || []).filter(visit => ['Missed Collection', 'Not Collected'].indexOf(visit.status) >= 0)
        .map(visit => ({visit}));
    }
  },

  methods: {
    options: function(visit) {
      return [
        {
          icon: 'eye',
          caption: this.$t('participants.view_visit'),
          onSelect: () => this._gotoVisit(visit)
        },
        {
          icon: 'redo',
          caption: this.$t('participants.new_visit'),
          onSelect: () => this.repeatVisit(visit)
        }
      ];
    },

    repeatVisit: async function(visit) {
      const wfInstanceSvc = this.$osSvc.tmWfInstanceSvc;
      if (wfInstanceSvc) {
        let wfName = await this._getCollectVisitsWf(visit);
        if (!wfName) {
          wfName = 'sys-collect-visits';
        }

        let inputItem = {cpr: {id: visit.cprId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle}};
        if (visit.eventId) {
          inputItem.cpe = {id: visit.eventId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle};
        }

        if (visit.id > 0) {
          inputItem.visit = {id: visit.id, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle};
        }

        const params = this._getBatchParams(visit, this._getVisitDescription(visit));
        const opts = {inputType: 'visit', params: {repeatVisit: true, ...params}};
        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
        wfInstanceSvc.gotoInstance(instance.id);
      } else {
        alert('Workflow module not installed!');
      }
    },

    onVisitRowClick: function({visit}) {
      this._gotoVisit(visit);
    },

    _getCollectVisitsWf(visit) {
      return cpSvc.getWorkflowProperty(visit.cpId, 'common', 'collectVisitsWf');
    },

    _getVisitDescription(visit) {
      let description = visit.description || 'Unknown';
      let idx = description.indexOf(' / ');
      if (idx >= 0) {
        description = description.substring(0, idx);
      }

      return description;
    },

    _getBatchParams(visit, title) {
      return {
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
        batchTitle: title,
        showOptions: false
      };
    },

    _gotoVisit: function({cpId, cprId, id, eventId}) {
      const route = routerSvc.getCurrentRoute();
      const params = {cpId, cprId, visitId: id || -1};
      if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
        routerSvc.goto('ParticipantsListItemVisitDetail.Overview', params, {eventId});
      } else {
        routerSvc.goto('VisitDetail.Overview', params, {eventId});
      }
    },
  }
}
</script>

<style scoped>
.os-missed-visits-tab :deep(.os-list .results .results-inner) {
  padding-right1: 0rem;
}


.os-missed-visits-tab :deep(table) {
  border: 0px;
  border-collapse: separate;
  border-spacing: 2px 15px;
  margin-top: -0.5rem;
}

.os-missed-visits-tab :deep(table > thead > tr > th) {
  background: transparent!important;
  border-bottom: 0;
  padding: 0rem 1rem;
  text-align: center;
}

.os-missed-visits-tab :deep(table tbody tr td) {
  border-top: 0;
}

.os-missed-visits-tab :deep(table tbody tr){
  box-shadow: 0 1px 2px 0 rgba(60,64,67,.3), 0 2px 6px 2px rgba(60,64,67,.15);
  border-radius: 0.5rem;
  background: transparent;
}

.os-missed-visits-tab :deep(table tbody tr:hover) {
  background: transparent;
}

.os-missed-visits-tab :deep(table tbody tr td) {
  vertical-align: middle;
  padding: 1rem;
}

</style>
