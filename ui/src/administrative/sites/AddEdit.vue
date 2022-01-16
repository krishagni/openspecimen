<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.site.id">Create Site</h3>
        <h3 v-else>Update {{dataCtx.site.name}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="siteForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="!dataCtx.site.id ? 'Create' : 'Update'" @click="saveOrUpdate" />
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

import siteSvc   from '@/administrative/services/Site.js';

export default {
  props: ['siteId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('SitesList', {siteId: -1}), label: 'Sites'}
      ],

      addEditFs: {rows: []}
    });

    let dataCtx = reactive({
      site: {},

      currentUser: ui.currentUser
    });

    let promises = [ siteSvc.getAddEditFormSchema() ];
    if (props.siteId && +props.siteId > 0) {
      promises.push(siteSvc.getSite(+props.siteId));
    }

    Promise.all(promises).then(
      function(result) {
        const { schema, defaultValues }   = result[0];
        ctx.addEditFs = schema;

        if (result.length > 1) {
          dataCtx.site = result[1];
          formUtil.createCustomFieldsMap(dataCtx.site);
        } else {
          if (Object.keys(defaultValues).length > 0) {
            dataCtx.site.extensionDetail = {attrsMap: defaultValues};
          }

          if (!ui.currentUser.admin) {
            dataCtx.site.instituteName = ui.currentUser.instituteName;
          }
        }
      }
    );

    return { ctx, dataCtx };
  },

  methods: {
    handleInput: function(event) {
      Object.assign(this.dataCtx, event.data);
    },

    saveOrUpdate: async function() {
      if (!this.$refs.siteForm.validate()) {
        return;
      }

      const savedSite = await siteSvc.saveOrUpdate(this.dataCtx.site);
      alertSvc.success('Site ' + savedSite.name + ' saved!');
      if (!this.dataCtx.site.id) {
        routerSvc.goto('SiteDetail.Overview', {siteId: savedSite.id});
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
