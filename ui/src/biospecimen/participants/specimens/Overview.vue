
<template>
  <os-page-toolbar>
    <template #default>
      <span> Action buttons </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />

      <SpecimenTree :cp="ctx.cp" :specimens="ctx.children" v-if="ctx.cp.id > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.auditObjs" v-if="ctx.specimen.id > 0" />
    </os-grid-column>
  </os-grid>
</template>

<script>

import SpecimenTree from '@/biospecimen/components/SpecimenTree.vue';

export default {
  props: ['specimen'],

  components: {
    SpecimenTree
  },

  inject: ['cpViewCtx'],

  data() {
    return {
      ctx: {
        cp: {},

        specimen: {},

        dict: [],

        auditObjs: [],

        routeQuery: this.$route.query,

        children: []
      }
    };
  },

  async created() {
    this._setupSpecimen();
    this.ctx.dict = await this.cpViewCtx.getSpecimenDict();
  },

  watch: {
    specimen: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupSpecimen();
      }
    }
  },

  methods: {
    _setupSpecimen: function() {
      this.cpViewCtx.getCp().then(cp => this.ctx.cp = cp);

      const specimen = this.ctx.specimen = this.specimen;
      if (specimen.id > 0) {
        this.ctx.auditObjs = [
          {objectId: specimen.id, objectName: 'specimen'}
        ]
      }

      this.ctx.children = specimen.children || [];
    }
  }
}
</script>
