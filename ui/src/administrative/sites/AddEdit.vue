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
          <Button :label="!dataCtx.site.id ? 'Create' : 'Update'" @click="saveOrUpdate"/>
          <Button label="Cancel" @click="cancel"/>
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import siteSchema    from '@/administrative/sites/schemas/site.js';
import addEditSchema from '@/administrative/sites/schemas/addedit.js';

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
        {url: routerSvc.getUrl('SitesList'), label: 'Sites'}
      ],

      addEditFs: {rows: []}
    });

    let dataCtx = reactive({
      site: {},

      currentUser: ui.currentUser
    });

    if (props.siteId && +props.siteId > 0) {
      siteSvc.getSite(+props.siteId).then(site => dataCtx.site = site);
    } else if (!ui.currentUser.admin) {
      dataCtx.site.instituteName = ui.currentUser.instituteName;
    }

    ctx.addEditFs = formUtil.getFormSchema(siteSchema.fields, addEditSchema.layout);
    return { ctx, dataCtx };
  },

  methods: {
    handleInput: function(event) {
      Object.assign(this.dataCtx, event.data);
    },

    saveOrUpdate: function() {
      if(!this.$refs.siteForm.validate()) {
        return;
      }

      siteSvc.saveOrUpdate(this.dataCtx.site).then(
        function(savedSite) {
          alertSvc.success('Site ' + savedSite.name + ' saved!');
          routerSvc.goto('SiteOverview', {siteId: savedSite.id});
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
