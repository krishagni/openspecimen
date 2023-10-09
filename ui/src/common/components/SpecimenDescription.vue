<template>
  <span class="description">
    <span class="status-ball" :class="statusColor" v-if="showStatus && statusColor"></span>
    <span>{{specimen.type}}</span>
  </span>
</template>

<script>

export default {
  props: ['specimen', 'show-status', 'status'],

  computed: {
    statusColor: function() {
      if (this.specimen.activityStatus == 'Closed') {
        return 'closed';
      } else if (!this.status || this.status == 'Pending') {
        return 'pending';
      } else if (this.status == 'Missed Collection' || this.status == 'Not Collected') {
        return 'not-collected';
      } else if (this.status == 'Collected') {
        return 'collected';
      }

      return null;
    }
  }
}

</script>

<style scoped>
.status-ball {
  display: inline-block;
  height: 0.75rem;
  width: 0.75rem;
  border-radius: 50%;
  background: #a0a0a0;
  margin-right: 0.25rem;
  margin-bottom: -0.0625rem;
}

.status-ball.collected {
  background: #5cb85c;
}

.status-ball.not-collected {
  background: #888;
}

.status-ball.pending {
  background: #f0ad4e;
}

.status-ball.closed {
  background: #d9534f!important;
}
</style>

