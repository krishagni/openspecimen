<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.dp.id">Create Distribution Protocol</h3>
        <h3 v-else>Update {{dataCtx.dp.shortTitle}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="dpForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="!dataCtx.dp.id ? 'Create' : 'Update'" @click="saveOrUpdate" />
          <os-button text label="Cancel" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc  from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import formUtil  from '@/common/services/FormUtil.js';
import util      from '@/common/services/Util.js';

import dpSvc   from '@/administrative/services/DistributionProtocol.js';

export default {
  props: ['dpId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('DpsList', {dpId: -1}), label: 'Distribution Protocols'}
      ],

      addEditFs: {rows: []}
    });

    let dataCtx = reactive({
      dp: {distributingSites: []},

      currentUser: ui.currentUser
    });

    let promises = [ dpSvc.getAddEditFormSchema() ];
    if (props.dpId && +props.dpId > 0) {
      promises.push(dpSvc.getDp(+props.dpId));
    }

    Promise.all(promises).then(
      function(result) {
        const { schema, defaultValues }   = result[0];
        ctx.addEditFs = schema;

        if (result.length > 1) {
          const dp = dataCtx.dp = result[1];
          formUtil.createCustomFieldsMap(dp);

          const sites = dp.distributingSites || {};
          dp.distributingSites = Object.keys(sites)
            .map(institute => ({institute: institute, sites: sites[institute].map(site => ({name: site}))}));

          if (!ui.currentUser.admin && dp.distributingSites.length > 1) {
            alertSvc.error('The distribution protocol can be edited only by the super admin');
            routerSvc.back();
          }
        } else {
          const dp = dataCtx.dp;
          dp.distributingSites = [{institute: ui.currentUser.instituteName, sites: []}];
          if (Object.keys(defaultValues).length > 0) {
            dp.extensionDetail = {attrsMap: defaultValues};
          }
        }
      }
    );

    return { ctx, dataCtx };
  },

  methods: {
    handleInput: function({field, data}) {
      Object.assign(this.dataCtx, data);

      const dp = this.dataCtx.dp;
      if (field.name == 'dp.instituteName') {
        dp.principalInvestigator = undefined;
        dp.coordinators = [];
        dp.defReceivingSiteName = undefined;
      }
    },

    saveOrUpdate: async function() {
      if (!this.$refs.dpForm.validate()) {
        return;
      }

      const toSave = util.clone(this.dataCtx.dp);
      toSave.distributingSites = toSave.distributingSites.reduce(
        (acc, el) => {
          acc[el.institute] = el.sites.map(site => site.name);
          return acc
        },
        {}
      );

      const savedDp = await dpSvc.saveOrUpdate(toSave);
      alertSvc.success('Distribution protocol ' + savedDp.shortTitle + ' saved!');
      if (!this.dataCtx.dp.id) {
        routerSvc.goto('DpDetail.Overview', {dpId: savedDp.id});
      } else {
        routerSvc.back();
      }
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
