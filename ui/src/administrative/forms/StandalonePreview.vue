<template>
  <os-page>
    <os-page-head>
      <h3>Preview: {{formDef.caption}}</h3>
    </os-page-head>

    <os-page-body>
      <FormPreview :form="{formId}" v-if="formId > 0" :key="formId" @form-definition="formDef = $event" />
    </os-page-body>
  </os-page>
</template>

<script>
import FormPreview from './Preview.vue';

export default {
  props: ['formId'],

  components: {
    FormPreview
  },

  data() {
    return {
      formDef: {}
    }
  }
}
</script>
