<template>
  <div>
    <table class="os-table muted-header os-border">
      <thead>
        <tr>
          <th>
            <span v-t="'visits.event'">Event Label</span>
          </th>
          <th>
            <span v-t="'visits.date'">Date</span>
          </th>
          <th>
            <span v-t="'visits.pending_specimens'">Pending Specimens</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(visit, $index) of pendingVisits" :key="$index">
          <td>
            <span>{{visit.eventLabel}}</span>
          </td>
          <td>
            <span>{{$filters.date(visit.visitDate || visit.anticipatedVisitDate)}}</span>
          </td>
          <td>
            <span>{{$filters.noValue(visit.pendingPrimarySpmns)}}</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  props: ['visits'],

  computed: {
    pendingVisits: function() {
      return (this.visits || []).filter(visit => !visit.status || visit.status == 'Pending');
    }
  }
}
</script>
