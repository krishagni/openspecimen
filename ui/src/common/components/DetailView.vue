<template>
  <div class="os-detail-view">
    <div class="tabs">
      <slot name="tabs"></slot>
    </div>
    <div class="content">
      <slot name="content"></slot>
    </div>
  </div>
</template>

<script>
export default {
}
</script>

<style scoped>
.os-detail-view {
  margin: -0.5rem 2.5rem 0rem 2.5rem;
  display: flex;
  flex-direction: column;
}

.os-detail-view .content {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.os-detail-view .content :deep(.os-page-toolbar) {
  margin-bottom: 1.25rem;
}

.os-detail-view .content :deep(.os-page-toolbar + .os-grid) {
  flex: 1;
}
</style>
