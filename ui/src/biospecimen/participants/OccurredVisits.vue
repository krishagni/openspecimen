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
        </os-button-group>
      </template>
    </os-list-view>
  </div>
</template>

<script>

import visitSvc from '@/biospecimen/services/Visit.js';

export default {
  props: ['cpr', 'visits'],

  data() {
    return {
      tabFields: []
    }
  },

  created() {
    visitSvc.getVisitsTab(this.cpr.cpId).then(
      (visitsTab) => {
        this.tabFields = visitsTab.occurred || [];
        if (this.tabFields.length == 0) {
          this.tabFields = [].concat(visitSvc.getDefaultOccurredVisitsTabFields());
        }

        this.tabFields.push({
          type: 'component',
          captionCode: 'visits.collection_stats',
          component: 'os-visit-specimen-collection-stats',
          data: (rowObject) => rowObject
        });

        this.tabFields.push({
          type: 'component',
          captionCode: 'visits.utilisation_stats',
          component: 'os-visit-specimen-utilisation-stats',
          data: (rowObject) => rowObject
        });
      }
    );
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
    }
  }
} 
</script>

<style scoped>
.os-occurred-visits-tab :deep(.os-list .results .results-inner) {
  padding-right: 0rem;
}
</style>
