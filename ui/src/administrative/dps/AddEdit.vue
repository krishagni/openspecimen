<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.dp.id">
          <span v-t="'dps.create'">Create Distribution Protocol</span>
        </h3>
        <h3 v-else>
          <span v-t="{path: 'common.update', args: {name: dataCtx.dp.shortTitle}}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="dpForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="$t(!dataCtx.dp.id ? 'common.buttons.create' : 'common.buttons.update')"
            @click="saveOrUpdate" />
          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc  from '@/common/services/Alerts.js';
import i18n      from '@/common/services/I18n.js';
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
        {url: routerSvc.getUrl('DpsList', {dpId: -1}), label: i18n.msg('dps.list')}
      ],

      addEditFs: {rows: []}
    });

    let dataCtx = reactive({
      dp: {distributingSites: []},

      currentUser: ui.currentUser,

      objName: 'dp',

      objCustomFields: 'dp.extensionDetail.attrsMap'
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
          dp.disableEmailNotifs = dp.disableEmailNotifs == true;
          formUtil.createCustomFieldsMap(dp);

          const sites = dp.distributingSites || {};
          dp.distributingSites = Object.keys(sites)
            .map(institute => ({institute: institute, sites: sites[institute].map(site => ({name: site}))}));

          if (!ui.currentUser.admin && dp.distributingSites.length > 1) {
            alertSvc.error({code: 'dps.admin_rights_req_to_edit'});
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

  computed: {
    noOfSites: function() {
      return (this.dataCtx.dp.distributingSites || []).length;
    }
  },

  watch: {
    noOfSites: function(newVal) {
      if (newVal == 0) {
        this.dataCtx.dp.distributingSites = [{institute: this.$ui.currentUser.instituteName, sites: []}];
      }
    }
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
          if (!el.institute || !el.sites || el.sites.length == 0) {
            return acc;
          }

          acc[el.institute] = el.sites.map(site => site.name);
          return acc
        },
        {}
      );

      const savedDp = await dpSvc.saveOrUpdate(toSave);
      alertSvc.success({code: 'dps.saved', args: savedDp});
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
