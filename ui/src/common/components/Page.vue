
<template>
  <div class="os-page">
    <slot></slot>
  </div>
</template>

<style scoped>
.os-page {
  height: 100%;
}

.os-page :deep(.os-page-body) {
  height: calc(100% - 107px);
  overflow-y: auto;
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
}
</style>
