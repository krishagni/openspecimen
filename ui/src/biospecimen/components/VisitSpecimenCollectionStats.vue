
<template>
  <div v-if="visit">
    <os-multi-progress-bar :total="visit.totalPrimarySpmns"
      :parts="[
        {value: visit.plannedPrimarySpmnsColl,   style: {background: '#5cb85c'}},
        {value: visit.unplannedPrimarySpmnsColl, style: {background: '#3b933b'}},
        {value: visit.uncollectedPrimarySpmns,   style: {background: '#888'}},
        {value: visit.pendingPrimarySpmns,       style: {background: '#f0ad4e'}}
      ]" @mouseover="showTooltip($event, visit)" @mouseleave="hideTooltip($event)" />

    <os-overlay ref="tooltip">
      <template #default>
        <table class="os-table os-border">
          <tbody>
            <tr v-if="visit.plannedPrimarySpmnsColl">
              <td v-t="'specimens.collected'">Collected</td>
              <td>{{visit.plannedPrimarySpmnsColl}}</td>
            </tr>
            <tr v-if="visit.unplannedPrimarySpmnsColl">
              <td v-t="'specimens.unplanned'">Unplanned</td>
              <td>{{visit.unplannedPrimarySpmnsColl}}</td>
            </tr>
            <tr v-if="visit.uncollectedPrimarySpmns">
              <td v-t="'specimens.missed_or_not_collected'">Not Collected</td>
              <td>{{visit.uncollectedPrimarySpmns}}</td>
            </tr>
            <tr v-if="visit.pendingPrimarySpmns">
              <td v-t="'specimens.pending'">Pending</td>
              <td>{{visit.pendingPrimarySpmns}}</td>
            </tr>
          </tbody>
        </table>
      </template>
    </os-overlay>
  </div>
</template>

<script>
export default {
  props: ['visit'],

  methods: {
    showTooltip: function(event) {
      this.$refs.tooltip.show(event);
    },

    hideTooltip: function(event) {
      this.$refs.tooltip.hide(event);
    }
  }
}
</script>
