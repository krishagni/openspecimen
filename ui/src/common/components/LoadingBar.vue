<template>
  <div class="os-loading-bar" v-show="pendingCount > 0">
    <ProgressBar mode="indeterminate" style="height: 0.25rem;" />
  </div>
</template>

<script>

import ProgressBar from 'primevue/progressbar';

export default {
  components: {
    ProgressBar
  },

  data() {
    return {
      pendingCount: 0
    }
  },

  methods: {
    increment: function() {
      if (this.pendingCount <= 0) {
        this.pendingCount = 1;
      } else {
        ++this.pendingCount;
      }
    },

    decrement: function() {
      --this.pendingCount;
      if (this.pendingCount < 0) {
        this.pendingCount = 0;
      }
    }
  }
}
</script>

<style scoped>
.os-loading-bar :deep(.p-progressbar .p-progressbar-value) {
  background: #b91f1f;
}
</style>
