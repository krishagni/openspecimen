<template>
  <os-page-toolbar>
    <template #default>
      <span>Action Buttons</span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>
  </os-grid>
</template>

<script>

// import cpSvc from '@/biospecimen/services/CollectionProtocol.js';

export default {
  props: ['cp'],

  inject: ['cpDict'],

  data() {
    return {
      ctx: {
        dict: []
      }
    };
  },

  created() {
    this.ctx.cp = this.cp;
    this.cpDict().then(dict => this.ctx.dict = dict);
  },

  computed: {
  },

  methods: {
  }
}
</script>
