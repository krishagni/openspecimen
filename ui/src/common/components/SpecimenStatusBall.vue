<template>
  <span class="status-ball" :class="statusColor"></span>
</template>

<script>
export default {
  props: ['specimen', 'status'],

  computed: {
    object: function() {
      return this.specimen || {type: 'Unknown Type'};
    },

    statusColor: function() {
      const status = this.status || this.object.availabilityStatus || this.object.status;
      switch (status) {
        case 'Distributed':
          return 'distributed';
        case 'Reserved':
          return 'reserved';
        case 'Closed':
          return 'closed';
        case 'Missed Collection':
        case 'Not Collected':
          return 'not-collected';
        case 'Available':
          return 'collected';
        case 'Pending':
        default:
          return 'pending';
      }
    },
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

.status-ball.distributed {
  background: #5bc0de;
}

.status-ball.reserved {
  background: rgba(128, 0, 128, 0.7);
}
</style>
