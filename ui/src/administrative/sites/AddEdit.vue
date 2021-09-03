<template>
  <Page>
    <PageHeader>
      <template #breadcrumb>
        <Breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.site.id">Create Site</h3>
        <h3 v-else>Update {{dataCtx.site.name}}</h3>
      </span>
    </PageHeader>

    <PageBody>
      <Form ref="siteForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <Button :label="!dataCtx.site.id ? 'Create' : 'Update'" @click="saveOrUpdate"/>
          <Button label="Cancel" @click="cancel"/>
        </div>
      </Form>
    </PageBody>
  </Page>
</template>

<script>
import { reactive, inject } from 'vue';

import Page       from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody   from '@/common/components/PageBody.vue';
import Button     from '@/common/components/Button.vue';
import Form       from '@/common/components/Form.vue';

import siteSchema    from '@/administrative/sites/schemas/site.js';
import addEditSchema from '@/administrative/sites/schemas/addedit.js';

import alertSvc  from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import formUtil  from '@/common/services/FormUtil.js';

import siteSvc   from '@/administrative/services/Site.js';

export default {
  props: ['siteId'],

  inject: ['ui'],

  components: {
    Page,
    PageHeader,
    Breadcrumb,
    PageBody,
    Form,
    Button
  },

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
