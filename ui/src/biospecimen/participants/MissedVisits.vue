<template>
  <div class="os-missed-visits-tab">
    <os-list-view
      class="os-muted-list-header os-bordered-list"
      :data="missedVisits"
      :schema="{columns: tabFields}"
      @rowClicked="onVisitRowClick"
      ref="listView" />
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
        this.tabFields = visitsTab.missed || [];
        if (this.tabFields.length == 0) {
          this.tabFields = [].concat(visitSvc.getDefaultMissedVisitsTabFields());
        }
      }
    );
  },

  computed: {
    missedVisits: function() {
      return (this.visits || []).filter(visit => ['Missed Collection', 'Not Collected'].indexOf(visit.status) >= 0)
        .map(visit => ({visit}));
    }
  }
}
</script>

<style scoped>
.os-missed-visits-tab :deep(.os-list .results .results-inner) {
  padding-right: 0rem;
}
</style>
