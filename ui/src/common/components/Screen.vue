
<template>
  <div class="os-screen p-grid" style="height: 100%;">
    <slot></slot>
  </div>
</template>

<script>

export default {
}

</script>

<style scoped>

.os-screen {
  margin-right: -1.5rem;
}

.os-screen :deep(.os-page-header) {
  border-bottom: 0;
}

.os-screen :deep(> div:not(:last-child) .os-page) {
  /* border-right: 1px solid #ddd; */
}

.os-screen :deep(> div:not(:first-child)) {
  margin-left: -1rem;
}

</style>
