<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb v-if="bcrumb">
        <os-breadcrumb :items="bcrumb" />
      </template>

      <h3 v-t="title"></h3>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12">
          <os-import-jobs :object-types="objectTypes" :object-params="objectParams" />
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
export default {
  props: ['bcrumb', 'title', 'object-types', 'object-params']
}
</script>
      
