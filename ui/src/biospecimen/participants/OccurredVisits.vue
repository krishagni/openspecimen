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
    }
  }
} 
</script>

<style scoped>
.os-occurred-visits-tab :deep(.os-list .results .results-inner) {
  padding-right: 0rem;
}
</style>
