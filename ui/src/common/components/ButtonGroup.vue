
<template>
  <div class="os-btn-group">
    <slot></slot>
  </div>
</template>

<script>
export default {
}
</script>

<style scoped>

.os-btn-group :deep(button:not(:last-child)) {
  border-top-right-radius: 0px;
  border-bottom-right-radius: 0px;
}

.os-btn-group :deep(button:not(:first-child)) {
  border-top-left-radius: 0px;
  border-bottom-left-radius: 0px;
  border-left: 0px;
}

.os-btn-group :deep(.icon-btn) {
  border: 1px solid #ccc;
}
</style>
