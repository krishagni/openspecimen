<template>
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
          <span v-t="'visits.reason'">Reason</span>
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="visit of missedVisits" :key="visit.id">
        <td>
          <span>{{visit.eventLabel}}</span>
        </td>
        <td>
          <span>{{$filters.date(visit.visitDate)}}</span>
        </td>
        <td>
          <span>{{$filters.noValue(visit.missedReason)}}</span>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script>
export default {
  props: ['visits'],

  computed: {
    missedVisits: function() {
      return (this.visits || []).filter(visit => ['Missed Collection', 'Not Collected'].indexOf(visit.status) >= 0);
    }
  }
}
</script>
