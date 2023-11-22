<template>
  <os-page-toolbar>
    <template #default>
      <span> Action buttons </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.auditObjs" v-if="ctx.cpr.id" />
    </os-grid-column>
  </os-grid>
</template>

<script>
import cprSvc from '@/biospecimen/services/Cpr.js';

export default {
  props: ['cpr'],

  data() {
    return {
      ctx: {
        cpr: {},

        dict: [],

        auditObjs: [],

        routeQuery: this.$route.query
      }
    };
  },

  async created() {
    this._setupCpr();
    this.ctx.dict = await cprSvc.getDict();
  },

  watch: {
    cpr: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupCpr();
      }
    }
  },

  methods: {
    _setupCpr: function() {
      const cpr = this.ctx.cpr = this.cpr;
      this.ctx.auditObjs = [
        {objectId: cpr.id, objectName: 'collection_protocol_registration'},
        {objectId: cpr.participant.id, objectName: 'participant'}
      ]
    }
  }
}
</script>
