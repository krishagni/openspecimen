
<template>
  <span class="os-badge">
    <slot />
  </span>
</template>

<script>
export default {
}
</script>

<style scoped>
.os-badge {
  display: inline-block;
  min-width: 0.625rem;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  font-weight: bold;
  line-height: 1;
  color: #fff;
  text-align: center;
  white-space: nowrap;
  vertical-align: middle;
  background-color: #777777;
  border-radius: 0.625rem;
}
</style>
