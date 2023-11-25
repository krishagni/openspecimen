<template>
  <div v-if="visit">
    <os-multi-progress-bar :total="visit.reqStorage"
      :parts="[
        {value: visit.storedSpecimens,      style: {background: '#5cb85c'}},
        {value: visit.notStoredSpecimens,   style: {background: '#99f'}},
        {value: visit.distributedSpecimens, style: {background: '#5bc0de'}},
        {value: visit.closedSpecimens,      style: {background: '#d9534f'}}
      ]" @mouseover="showTooltip($event)" @mouseleave="hideTooltip($event)" />

    <os-overlay ref="tooltip">
      <template #default>
        <table class="os-table os-border">
          <tbody>
            <tr v-if="!visit.reqStorage">
              <td colspan="2">
                <span v-t="'specimens.no_specimens'">No Specimens</span>
              </td>
            </tr>
            <tr v-if="visit.storedSpecimens">
              <td v-t="'specimens.stored'">Stored</td>
              <td>{{visit.storedSpecimens}}</td>
            </tr>
            <tr v-if="visit.notStoredSpecimens">
              <td v-t="'specimens.not_stored'">Not Stored</td>
              <td>{{visit.notStoredSpecimens}}</td>
            </tr>
            <tr v-if="visit.distributedSpecimens">
              <td v-t="'specimens.distributed'">Distributed</td>
              <td>{{visit.distributedSpecimens}}</td>
            </tr>
            <tr v-if="visit.closedSpecimens">
              <td v-t="'specimens.closed'">Closed</td>
              <td>{{visit.closedSpecimens}}</td>
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
