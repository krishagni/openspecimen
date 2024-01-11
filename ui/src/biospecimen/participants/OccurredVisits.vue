<template>
  <div class="os-occurred-visits-tab">
    <os-list-view
      class="os-muted-list-header os-bordered-list"
      :data="occurredVisits"
      :schema="{columns: tabFields}"
      :showRowActions="true"
      @rowClicked="onVisitRowClick"
      ref="listView">

      <template #rowActions="{rowObject}">
        <os-button-group>
          <os-button left-icon="file" size="small" v-if="rowObject.visit.sprName" @click="showReport(rowObject)" />

          <os-menu icon="ellipsis-v" :no-outline="true" :options="options(rowObject.visit)" />
        </os-button-group>
      </template>
    </os-list-view>
  </div>
</template>

<script>

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import routerSvc from '@/common/services/Router.js';
import util from '@/common/services/Util.js';

export default {
  props: ['visits'],

  inject: ['cpViewCtx'],

  data() {
    return {
      tabFields: []
    }
  },

  created() {
    this.cpViewCtx.getOccurredVisitsTabFields().then(tabFields => this.tabFields = tabFields);
  },

  computed: {
    occurredVisits: function() {
      return (this.visits || []).filter(visit => visit.status == 'Complete').map(visit => ({visit}));
    }
  },

  methods: {
    onVisitRowClick: function({visit}) {
      this.gotoVisit(visit);
    },

    showReport: function({visit}) {
      alert('Show visit report: ' + visit.name);
    },

    options: function(visit) {
      return [
        {
          icon: 'eye',
          caption: this.$t('participants.view_visit'),
          onSelect: () => this.gotoVisit(visit)
        },
        {
          icon: 'redo',
          caption: this.$t('participants.new_visit'),
          onSelect: () => this.repeatVisit(visit)
        },
        { divider: true },
        {
          icon: 'flask',
          caption: this.$t('participants.collect_pending_specimens'),
          onSelect: () => this.collectPending(visit)
        },
        {
          icon: 'plus',
          caption: this.$t('participants.collect_unplanned_specimens'),
          onSelect: () => this.addSpecimen(visit)
        },
        {
          icon: 'print',
          caption: this.$t('participants.print_specimen_labels'),
          onSelect: () => this.printLabels(visit)
        }
      ];
    },

    gotoVisit: function(visit) {
      const route = routerSvc.getCurrentRoute();
      const params = {
        cpId: visit.cpId,
        cprId: visit.cprId,
        visitId: visit.id || -1,
        eventId: visit.eventId
      };

      if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
        routerSvc.goto('ParticipantsListItemVisitDetail.Overview', params);
      } else {
        routerSvc.goto('VisitDetail.Overview', params);
      }
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

        const params = {repeatVisit: true, ...this._getBatchParams(visit, this._getVisitDescription(visit))};
        const opts = {inputType: 'visit', params};
        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
        wfInstanceSvc.gotoInstance(instance.id);
      } else {
        alert('Workflow module not installed!');
      }
    },

    collectPending: async function(visit) {
      const wfInstanceSvc = this.$osSvc.tmWfInstanceSvc;
      if (wfInstanceSvc) {
        let wfName = await this._getCollectPendingSpmnsWf(visit);
        if (!wfName) {
          wfName = 'sys-collect-pending-specimens';
        }

        let inputItem = {
          cpr: {id: visit.cprId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle},
          visit: {id: visit.id, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle}
        };
        if (visit.eventId) {
          inputItem.cpe = {id: visit.eventId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle};
        }

        const opts = {params: this._getBatchParams(visit, this.$t('participants.collect_specimens'))};
        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
        wfInstanceSvc.gotoInstance(instance.id);
      } else {
        alert('Workflow module not installed!');
      }
    },

    addSpecimen: async function(visit) {
      const wfInstanceSvc = this.$osSvc.tmWfInstanceSvc;
      if (wfInstanceSvc) {
        let wfName = await this._getCollectUnplannedSpmnsWf(visit);
        if (!wfName) {
          wfName = 'sys-collect-adhoc-specimens';
        }

        let inputItem = {
          cpr: {id: visit.cprId, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle},
          visit: {id: visit.id, cpId: visit.cpId, cpShortTitle: visit.cpShortTitle}
        };

        const params = this._getBatchParams(visit, this.$t('participants.add_specimen'));
        params['breadcrumb-3'] = JSON.stringify({
          label: this._getVisitDescription(visit),
          route: {
            name: 'ParticipantsListItemVisitDetail.Overview',
            params: {cpId: visit.cpId, cprId: visit.cprId, visitId: visit.id}
          }
        });

        const opts = {inputType: 'visit', params};
        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem], opts);
        wfInstanceSvc.gotoInstance(instance.id);
      } else {
        alert('Workflow module not installed!');
      }
    },

    printLabels: function(visit) {
      const ts = util.formatDate(new Date(), 'yyyyMMdd_HHmmss');
      const outputFilename = [visit.cpShortTitle, visit.ppid, visit.name, ts].join('_') + '.csv';
      specimenSvc.printLabels({visitId: visit.id}, outputFilename);
    },

    _getCollectVisitsWf(visit) {
      return cpSvc.getWorkflowProperty(visit.cpId, 'common', 'collectVisitsWf');
    },

    _getCollectPendingSpmnsWf(visit) {
      return cpSvc.getWorkflowProperty(visit.cpId, 'common', 'collectPendingSpecimensWf');
    },

    _getCollectUnplannedSpmnsWf(visit) {
      return cpSvc.getWorkflowProperty(visit.cpId, 'common', 'collectUnplannedSpecimensWf');
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
    }
  }
} 
</script>

<style scoped>
.os-occurred-visits-tab :deep(.os-list .results .results-inner) {
  padding-right: 0rem;
}
</style>
