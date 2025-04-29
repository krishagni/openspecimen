<template>
  <os-page-toolbar>
    <template #default>
      <span>action buttons</span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <span>{{cpg.name}} details</span>
    </os-grid-column>
  </os-grid>
</template>

<script>
export default {
  props: ['cpg']
}
</script>
