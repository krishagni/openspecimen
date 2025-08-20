<template>
  <div class="os-progress-bar" v-os-tooltip.bottom="tooltip">
    <ProgressBar :value="completed" :show-value="false">
    </ProgressBar>
    <div class="value">
      <slot>{{value}} / {{total}}</slot>
    </div>
  </div>
</template>

<script>

import ProgressBar from 'primevue/progressbar';

export default {
  props: ['value', 'total', 'tooltip'],

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
.os-progress-bar {
  position: relative;
}

.os-progress-bar .value {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
}

.os-progress-bar :deep(.p-progressbar) {
  background: #f0ad4e;
}

.os-progress-bar :deep(.p-progressbar .p-progressbar-value) {
  background: #5cb85c;
}
</style>
