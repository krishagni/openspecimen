
<template>
  <os-page-toolbar>
    <template #default>
      <span> Action buttons </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <pre>{{visit}}</pre>
    </os-grid-column>
  </os-grid>
</template>

<script>
export default {
  props: ['visit']
}
</script>
