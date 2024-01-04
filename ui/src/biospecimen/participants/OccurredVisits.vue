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

export default {
  props: ['visits'],

  inject: ['cpViewCtx'],

  data() {
    return {
      tabFields: []
    }
  },

  created() {
    this.cpViewCtx.value.getOccurredVisitsTabFields().then(tabFields => this.tabFields = tabFields);
  },

  computed: {
    occurredVisits: function() {
      return (this.visits || []).filter(visit => visit.status == 'Complete').map(visit => ({visit}));
    }
  },

  methods: {
    onVisitRowClick: function(row) {
      console.log(row);
    },

    showReport: function({visit}) {
      alert('Show visit report: ' + visit.name);
    },

    options: function(visit) {
      return [
        {
          icon: 'flask',
          caption: this.$t('participants.collect_pending_specimens'),
          onSelect: () => this.collectPending(visit)
        }
      ];
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

        const instance = await wfInstanceSvc.createInstance({name: wfName}, null, null, null, [inputItem]);
        wfInstanceSvc.gotoInstance(instance.id);
      } else {
        alert('Workflow module not installed!');
      }
    },

    _getCollectPendingSpmnsWf(visit) {
      return cpSvc.getWorkflowProperty(visit.cpId, 'common', 'collectPendingSpecimensWf');
    },
  }
} 
</script>

<style scoped>
.os-occurred-visits-tab :deep(.os-list .results .results-inner) {
  padding-right: 0rem;
}
</style>
