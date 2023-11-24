<template>
  <div>
    <table class="os-table muted-header os-border">
      <thead>
        <tr>
          <th>
            <span v-t="'visits.event'">Event Label</span>
          </th>
          <th>
            <span v-t="'visits.name'">Name</span>
          </th>
          <th>
            <span v-t="'visits.date'">Date</span>
          </th>
          <th>
            <span v-t="'visits.collection_stats'">Collection Stats</span>
          </th>
          <th>
            <span v-t="'visits.utilisation_stats'">Utilization Stats</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="visit of occurredVisits" :key="visit.id">
          <td>
            <span>{{visit.eventLabel}}</span>
          </td>
          <td>
            <span>{{visit.name}}</span>
          </td>
          <td>
            <span>{{$filters.date(visit.visitDate)}}</span>
          </td>
          <td>
            <os-multi-progress-bar :total="visit.totalPrimarySpmns" 
              :parts="[
                {value: visit.plannedPrimarySpmnsColl,   style: {background: '#5cb85c'}},
                {value: visit.unplannedPrimarySpmnsColl, style: {background: '#3b933b'}},
                {value: visit.uncollectedPrimarySpmns,   style: {background: '#888'}},
                {value: visit.pendingPrimarySpmns,       style: {background: '#f0ad4e'}}
              ]" @mouseover="showSpecimenDetails($event, visit)" @mouseleave="hideSpecimenDetails($event)" />
          </td>
          <td>
            <os-multi-progress-bar :total="visit.reqStorage"
              :parts="[
                {value: visit.storedSpecimens,      style: {background: '#5cb85c'}},
                {value: visit.notStoredSpecimens,   style: {background: '#99f'}},
                {value: visit.distributedSpecimens, style: {background: '#5bc0de'}},
                {value: visit.closedSpecimens,      style: {background: '#d9534f'}}
              ]" @mouseover="showSpecimenUtilisation($event, visit)" @mouseleave="hideSpecimenUtilisation($event)" />
          </td>
        </tr>
      </tbody>
    </table>

    <os-overlay ref="visitSpmnsStat">
      <template #default>
        <table class="os-table os-border">
          <tbody>
            <tr v-if="selectedVisit.plannedPrimarySpmnsColl">
              <td v-t="'specimens.collected'">Collected</td>
              <td>{{selectedVisit.plannedPrimarySpmnsColl}}</td>
            </tr>
            <tr v-if="selectedVisit.unplannedPrimarySpmnsColl">
              <td v-t="'specimens.unplanned'">Unplanned</td>
              <td>{{selectedVisit.unplannedPrimarySpmnsColl}}</td>
            </tr>
            <tr v-if="selectedVisit.uncollectedPrimarySpmns">
              <td v-t="'specimens.missed_or_not_collected'">Not Collected</td>
              <td>{{selectedVisit.uncollectedPrimarySpmns}}</td>
            </tr>
            <tr v-if="selectedVisit.pendingPrimarySpmns">
              <td v-t="'specimens.pending'">Pending</td>
              <td>{{selectedVisit.pendingPrimarySpmns}}</td>
            </tr>
          </tbody>
        </table>
      </template>
    </os-overlay>

    <os-overlay ref="visitSpmnsStorage">
      <template #default>
        <table class="os-table os-border">
          <tbody>
            <tr v-if="!selectedVisit.reqStorage">
              <td colspan="2">
                <span v-t="'specimens.no_specimens'">No Specimens</span>
              </td>
            </tr>
            <tr v-if="selectedVisit.storedSpecimens">
              <td v-t="'specimens.stored'">Stored</td>
              <td>{{selectedVisit.storedSpecimens}}</td>
            </tr>
            <tr v-if="selectedVisit.notStoredSpecimens">
              <td v-t="'specimens.not_stored'">Not Stored</td>
              <td>{{selectedVisit.notStoredSpecimens}}</td>
            </tr>
            <tr v-if="selectedVisit.distributedSpecimens">
              <td v-t="'specimens.distributed'">Distributed</td>
              <td>{{selectedVisit.distributedSpecimens}}</td>
            </tr>
            <tr v-if="selectedVisit.closedSpecimens">
              <td v-t="'specimens.closed'">Closed</td>
              <td>{{selectedVisit.closedSpecimens}}</td>
            </tr>
          </tbody>
        </table>
      </template>
    </os-overlay>
  </div>
</template>

<script>
export default {
  props: ['visits'],

  data() {
    return {
      selectedVisit: null
    }
  },

  computed: {
    occurredVisits: function() {
      return (this.visits || []).filter(visit => visit.status == 'Complete');
    }
  },

  methods: {
    showSpecimenDetails: async function(event, visit) {
      this.selectedVisit = visit;
      this.$refs.visitSpmnsStat.show(event);
    },

    hideSpecimenDetails: async function(event) {
      this.selectedVisit = null;
      this.$refs.visitSpmnsStat.hide(event);
    },

    showSpecimenUtilisation: async function(event, visit) {
      this.selectedVisit = visit;
      this.$refs.visitSpmnsStorage.show(event);
    },

    hideSpecimenUtilisation: async function(event) {
      this.selectedVisit = null;
      this.$refs.visitSpmnsStorage.hide(event);
    }
  }
} 
</script>

