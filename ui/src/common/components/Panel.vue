
<template>
  <Panel class="os-panel">
    <template #header>
      <slot name="header"></slot>
    </template>

    <template #icons>
      <slot name="actions"></slot>
    </template>

    <slot></slot>
  </Panel>
</template>

<script>

import Panel from 'primevue/panel';

export default {

  components: {
    Panel
  }

}

</script>

<style scoped>

.os-panel {
  height: 100%;
}

.os-panel :deep(.p-panel-header) {
  padding: 0.5rem 1rem;
}

.os-panel :deep(.p-toggleable-content) {
  height: 100%;
}

.os-panel :deep(.p-panel-content) {
  padding: 1rem;
  height: calc(100% - 42px);
  overflow-y: auto;
}

.os-panel :deep(.p-panel-header .title) {
  padding-right: 15px;
}

.os-panel :deep(.p-panel-header button) {
  margin-right: 0.25rem;
}
</style>
