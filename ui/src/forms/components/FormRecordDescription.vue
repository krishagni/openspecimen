<template>
  <span class="record-description">
    <span :class="statusBall" v-os-tooltip.bottom="statusTooltip"></span>
    <span>{{description}}</span>
  </span>
</template>

<script>
export default {
  props: ['record'],

  computed: {
    statusBall: function() {
      return ['status-ball', this.record.formStatus == 'DRAFT' ? 'draft' : 'completed'];
    },

    statusTooltip: function() {
      return this.$t(this.record.formStatus == 'DRAFT' ? 'common.draft' : 'common.completed');
    },

    description: function() {
      const {recordId, formCaption} = this.record;
      return '#' + recordId + ' ' + formCaption;
    }
  }
}
</script>

<style scoped>
.record-description {
  display: flex;
  align-items: center;
}

.status-ball {
  display: inline-block;
  min-height: 0.75rem;
  min-width: 0.75rem;
  border-radius: 50%;
  background: #a0a0a0;
  margin-right: 0.5rem;
}

.status-ball.draft {
  background: #f0ad4e;
}

.status-ball.completed {
  background: #5cb85c;
}
</style>
