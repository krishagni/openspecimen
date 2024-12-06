<template>
  <os-form ref="reqForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
    <div>
      <os-button primary :label="$t(!dataCtx.requirement.id ? 'common.buttons.create' : 'common.buttons.update')"
        @click="saveOrUpdate" />
      <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
    </div>
  </os-form>
</template>

<script>
import ui        from '@/global.js';
import alertsSvc from '@/common/services/Alerts.js';
import formUtil  from '@/common/services/FormUtil.js';
import routerSvc from '@/common/services/Router.js';
import dpSvc     from '@/administrative/services/DistributionProtocol.js';

export default {
  props: ['dp', 'reqId'],

  data() {
    return {
      dataCtx: {
        requirement: {dp: {id: this.dp.id}},

        invoicingEnabled: ui.global.appProps.plugins.indexOf('distribution-invoicing') != -1,

        objName: 'requirement',

        objCustomFields: 'requirement.extensionDetail.attrsMap'
      },

      ctx: {
        addEditFs: { rows: [] }
      }
    }
  },

  async created() {
    const {schema, defaultValues} = await dpSvc.getRequirementAddEditFormSchema();

    this.ctx.addEditFs = schema;
    this.ctx.defaultValues = defaultValues;
    this.loadRequirement();
  },

  watch: {
    reqId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadRequirement();
      }
    }
  },

  methods: {
    handleInput: function({data}) {
      Object.assign(this.dataCtx, data);
    },

    loadRequirement: async function() {
      if (this.reqId && +this.reqId > 0) {
        const requirement = this.dataCtx.requirement = await dpSvc.getRequirement(this.reqId);
        formUtil.createCustomFieldsMap(requirement);
      } else {
        this.dataCtx.requirement = {dp: {id: this.dp.id}},
        this.dataCtx.requirement.extensionDetail = {attrsMap: this.defaultValues};
      }
    },

    saveOrUpdate: async function() {
      if (!this.$refs.reqForm.validate()) {
        return;
      }

      await dpSvc.saveOrUpdateReq(this.dataCtx.requirement);
      alertsSvc.success({code: 'dps.req_saved'});
      this.cancel();
    },

    cancel: function() {
      const route = this.$route.matched[this.$route.matched.length - 1];
      const views  = route.name.split('.');
      routerSvc.goto(views[0] + '.Requirements.List', {dpId: this.dp.id});
    }
  }
}
</script>
