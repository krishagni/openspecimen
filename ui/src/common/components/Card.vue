<template>
  <div class="os-card">
    <div class="header" v-if="$slots.header">
      <slot name="header"></slot>
    </div>
    <div class="body">
      <slot name="body"></slot>
    </div>
  </div>
</template>

<script>
export default {
}
</script>

<style scoped>
.os-card {
  box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .3), 0 2px 6px 2px rgba(60, 64, 67, .15);
  border-radius: 0.5rem;
  background: transparent;
  padding: 1rem;
}

.os-card .header {
  margin-bottom: 1rem;
}

.os-card .header :deep(.toolbar) {
  display: flex;
  align-items: center;
}

.os-card .header :deep(.title) {
  font-weight: bold;
  flex: 1;
}

.os-card .header :deep(.toolbar > *:first-child) {
  flex: 1;
}

.os-card .header :deep(.os-icon-wrapper) {
  margin-right: 0.5rem;
}

.os-card .header :deep(.actions button) {
  border: 0;
}
</style>
