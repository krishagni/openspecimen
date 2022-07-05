<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="'admin'">
        <os-button left-icon="edit" :label="$t('common.buttons.edit')"
          @click="$goto('InstituteAddEdit', {instituteId: ctx.institute.id})" />

        <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteInstitute" />
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.instituteObjs" v-if="ctx.institute.id" />
    </os-grid-column>
  </os-grid>

  <os-delete-object ref="deleteObj" :input="ctx.deleteOpts" />
</template>

<script>
import routerSvc    from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  props: ['institute'],

  inject: ['ui'],

  data() {
    return {
      ctx: {
        institute: {},

        deleteOpts: {},

        instituteObjs: [],

        dict: []
      }
    };
  },

  created() {
    let setup = () => {
      this.ctx.institute = this.institute;
      this.ctx.deleteOpts = {
        type: this.$t('institutes.singular'),
        title: this.institute.name,
        dependents: () => instituteSvc.getDependents(this.institute),
        deleteObj: () => instituteSvc.delete(this.institute)
      };
      this.ctx.instituteObjs = [{objectName: 'institute', objectId: this.institute.id}];
      instituteSvc.getDict().then(dict => this.ctx.dict = dict);
    }

    this.$watch(() => this.institute, setup);
    setup();
  },

  methods: {
    deleteInstitute: function() {
      this.$refs.deleteObj.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('InstitutesList', {instituteId: -2});
          }
        }
      );
    }
  }
}
</script>
