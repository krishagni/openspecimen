
<template>
  <span :class="containerClass">
    <Tag :value="value" :severity="type" :rounded="rounded"></Tag>
  </span>
</template>

<script>
import Tag from 'primevue/tag';

export default {
  props: ['value', 'type', 'rounded'],

  components: {
    Tag
  },

  computed: {
    containerClass: function() {
      return ['os-tag', {
        'os-missed-tag': this.type == 'missed' || this.type == 'Missed',
        'os-distributed-tag': this.type == 'distributed' || this.type == 'Distributed',
        'os-reserved-tag': this.type == 'reserved' || this.type == 'Reserved',
        'os-closed-tag': this.type == 'closed' || this.type == 'Closed'
      }]
    }
  }
}
</script>

<style scoped>
.os-tag :deep(.p-tag.p-tag-warning) {
  background-color: #f59e0b;
  color: #fff;
}

.os-tag :deep(.p-tag.p-tag-secondary) {
  background-color: #020617;
  color: #fff;
}

.os-tag.os-missed-tag :deep(.p-tag) {
  background-color: #888;
  color: #fff;
}

.os-tag.os-distributed-tag :deep(.p-tag) {
  background-color: #5bc0de;
  color: #fff;
}

.os-tag.os-reserved-tag :deep(.p-tag) {
  background-color: rgba(128, 0, 128, 0.7);
  color: #fff;
}

.os-tag.os-closed-tag :deep(.p-tag) {
  background-color: #d9534f;
  color: #fff;
}
</style>
