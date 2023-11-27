<template>
  <div class="os-pending-visits-tab">
    <os-list-view
      class="os-muted-list-header os-bordered-list"
      :data="pendingVisits"
      :schema="{columns: tabFields}"
      :showRowActions="true"
      @rowClicked="onVisitRowClick"
      ref="listView" />
  </div>
</template>

<script>

export default {
  props: ['cpr', 'visits'],

  inject: ['cpViewCtx'],

  data() {
    return {
      tabFields: []
    }
  },

  created() {
    this.cpViewCtx.value.getPendingVisitsTabFields().then(tabFields => this.tabFields = tabFields);
  },

  computed: {
    pendingVisits: function() {
      return (this.visits || []).filter(visit => !visit.status || visit.status == 'Pending')
        .map(visit => ({
          visit: Object.assign(
            visit, {cprId: this.cpr.id, anticipatedVisitDate: this._getAnticipatedVisitDate(visit)})
        }));
    }
  },

  methods: {
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
