
<template>
  <Panel class="os-panel">
    <template #header>
      <slot name="header"></slot>

      <slot name="primary-actions"></slot>
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
  display: flex;
  flex-direction: column;
}

.os-panel.os-full-height-panel {
  height: 100%;
}

.os-panel :deep(.p-panel-header) {
  padding: 0.5rem 1rem;
  display: flex;
  flex-direction: row;
}

.os-panel :deep(.p-panel-header .p-panel-icons) {
  flex: 1;
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
}

.os-panel :deep(.p-toggleable-content) {
  flex: 1;
  overflow-y: auto;
}

.os-panel :deep(.p-panel-content) {
  padding: 1rem;
}

.os-panel :deep(.p-panel-header .title) {
  padding-right: 15px;
}

.os-panel :deep(.p-panel-header button.btn),
.os-panel :deep(.p-panel-header .os-btn-group) {
  margin-right: 0.25rem;
}

.os-panel :deep(.p-panel-header .os-btn-group button.btn) {
  margin-right: 0rem;
}
</style>
