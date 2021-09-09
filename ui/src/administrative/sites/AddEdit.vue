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
          <os-button :label="!dataCtx.site.id ? 'Create' : 'Update'" @click="saveOrUpdate" />
          <os-button label="Cancel" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import siteSchema    from '@/administrative/schemas/sites/site.js';
import addEditSchema from '@/administrative/schemas/sites/addedit.js';

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

      addEditFs: formUtil.getFormSchema(siteSchema.fields, addEditSchema.layout)
    });

    let dataCtx = reactive({
      site: {},

      currentUser: ui.currentUser
    });

    let promises = [ siteSvc.getCustomFieldsForm() ];
    if (props.siteId && +props.siteId > 0) {
      promises.push(siteSvc.getSite(+props.siteId));
    }

    Promise.all(promises).then(
      function(result) {
        const { schema, defaultValues }   = formUtil.fromDeToStdSchema(result[0], 'site.extensionDetail.attrsMap.');
        ctx.addEditFs.rows = ctx.addEditFs.rows.concat(schema.rows);

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

    saveOrUpdate: function() {
      if (!this.$refs.siteForm.validate()) {
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
