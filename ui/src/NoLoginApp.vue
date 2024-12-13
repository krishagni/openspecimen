<template>
  <div class="os-root">
    <os-navbar :no-login="true" />

    <os-app-body />
  </div>
</template>

<script>
export default {
  name: 'NoLoginApp'
}
</script>

<style scoped>
.os-root {
  height: 100%;
}
</style>
