
<template>
  <div class="os-page-toolbar">
    <div class="left">
      <slot></slot>
    </div>
    <div class="right">
      <slot name="right"></slot>
    </div>
  </div>
</template>

<style scoped>
  .os-page-toolbar {
    padding: 10px 0px 15px;
    display: flex;
  }

  .os-page-toolbar:after {
    content: ' ';
    clear: both;
    display: block;
  }

  .os-page-toolbar :deep(a.button-link),
  .os-page-toolbar :deep(button) {
    margin-right: 0.5rem;
  }

  .os-page-toolbar :deep(.os-btn-group) {
    display: inline-block;
    margin-right: 0.5rem;
  }

  .os-page-toolbar :deep(.os-btn-group button) {
    margin: 0;
  }

  .os-page-toolbar .left {
    margin-right: 10px;
    flex: 1;
  }

  .os-page-toolbar .right {
    margin-left: auto;
    margin-right: 15px;
  }
</style>
