<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" label="Edit" @click="$goto('SiteAddEdit', {siteId: ctx.site.id})" />

      <os-button left-icon="trash" label="Delete" @click="deleteSite" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="siteSchema.fields" :object="ctx" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.siteObjs" v-if="ctx.site.id" />
    </os-grid-column>
  </os-grid>

  <os-delete-object ref="deleteObj" :input="ctx.deleteOpts" />
</template>

<script>
import { reactive, watchEffect } from 'vue';

import siteSchema from '@/administrative/sites/schemas/site.js';

import routerSvc from '@/common/services/Router.js';
import siteSvc from '@/administrative/services/Site.js';

export default {
  props: ['site'],

  inject: ['ui'],

  setup(props) {
    let ctx = reactive({
      site: {},

      deleteOpts: {},

      siteObjs: []
    });

    
    watchEffect(
      () => {
        ctx.site = props.site;
        ctx.deleteOpts = {
          type: 'Site',
          title: props.site.name,
          dependents: () => siteSvc.getDependents(props.site),
          deleteObj: () => siteSvc.delete(props.site)
        };
        ctx.siteObjs = [{objectName: 'site', objectId: props.site.id}];
      }
    );

    return { ctx, siteSchema };
  },

  computed: {
  },

  methods: {
    deleteSite: function() {
      this.$refs.deleteObj.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('SitesList');
          }
        }
      );
    }
  }
}
</script>
