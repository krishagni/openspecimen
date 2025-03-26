<template>
  <div class="os-root">
    <os-app-body />
  </div>
</template>

<script>
export default {
  name: 'NoLoginApp',
}
</script>

<style scoped>
.os-root {
  height: 100%;
}

.os-root :deep(.os-app-body) {
  height: 100%;
}
</style>
