
<template>
  <div class="os-page-body">
    <slot></slot>
  </div>
</template>

<style scoped>
.os-page-body {
  position: relative;
  padding: 10px 15px 0px;
  display: flex;
  flex-direction: column;
}

.os-page-body :deep(.os-page-toolbar) {
  max-height: 55px;
}

.os-page-body :deep(> div:not(.os-page-toolbar)) {
  height: 100%;
  flex: 1;
}

.os-page-body :deep(.p-grid) {
  height: 100%;
}

.os-page-body :deep(.os-page-toolbar + div) {
  height: calc(100% - 55px);
}


.os-page-body :deep(.os-side-menu ~ div) {
  margin-left: 64px;
  transition: all 0.3s ease-in-out;
}

.os-page-body :deep(.os-side-menu:hover ~ div) {
  margin-left: 225px;
}
</style>
