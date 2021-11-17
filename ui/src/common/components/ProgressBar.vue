<template>
  <div class="os-progress-bar">
    <ProgressBar :value="completed">
      <slot></slot>
    </ProgressBar>
  </div>
</template>

<script>

import ProgressBar from 'primevue/progressbar';

export default {
  props: ['value', 'total'],

  components: {
    ProgressBar
  },

  computed: {
    completed: function() {
      return this.total > 0 ? Math.round((this.value || 0) / this.total * 100) : 0;
    }
  }
}

</script>

<style scoped>

.os-progress-bar :deep(.p-progressbar .p-progressbar-value) {
  background: #5cb85c;
}

</style>
