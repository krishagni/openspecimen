
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
      return ['os-tag', {'os-missed-tag': this.type == 'missed' || this.type == 'Missed'}]
    }
  }
}
</script>

<style scoped>
.os-tag :deep(.p-tag.p-tag-warning) {
  background-color: #f59e0b;
  color: #fff;
}

.os-tag.os-missed-tag :deep(.p-tag) {
  background-color: #888;
  color: #fff;
}
</style>
