<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="'institute-admin'">
        <os-button left-icon="edit" label="Edit" @click="$goto('SiteAddEdit', {siteId: ctx.site.id})" />

        <os-button left-icon="trash" label="Delete" @click="deleteSite" />
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.siteObjs" v-if="ctx.site.id" />
    </os-grid-column>
  </os-grid>

  <os-delete-object ref="deleteObj" :input="ctx.deleteOpts" />
</template>

<script>
import { reactive, watchEffect } from 'vue';

import routerSvc from '@/common/services/Router.js';
import siteSvc from '@/administrative/services/Site.js';

export default {
  props: ['site'],

  inject: ['ui'],

  setup(props) {
    let ctx = reactive({
      site: {},

      deleteOpts: {},

      siteObjs: [],

      dict: []
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

    siteSvc.getDict().then(dict => ctx.dict = dict);
    return { ctx };
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
