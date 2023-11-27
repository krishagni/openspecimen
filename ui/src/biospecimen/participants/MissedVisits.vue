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

export default {
  props: ['visits'],

  inject: ['cpViewCtx'],

  data() {
    return {
      tabFields: []
    }
  },

  created() {
    this.cpViewCtx.value.getMissedVisitsTabFields().then(tabFields => this.tabFields = tabFields);
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
