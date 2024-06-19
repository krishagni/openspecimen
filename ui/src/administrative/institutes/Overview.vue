<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="'admin'">
        <os-button left-icon="edit" :label="$t('common.buttons.edit')"
          @click="$goto('InstituteAddEdit', {instituteId: ctx.institute.id})" />

        <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteInstitute" />

        <os-button left-icon="history" :label="$t('audit.trail')" @click="viewAuditTrail" />
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>
  </os-grid>

  <os-delete-object ref="deleteObj" :input="ctx.deleteOpts" />

  <os-audit-trail ref="auditTrailDialog" :objects="ctx.instituteObjs" />
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
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    }
  }
}
</script>
