<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editCp"
        v-show-if-allowed="cpResources.updateOpts" />

      <os-button left-icon="copy" :label="$t('common.buttons.clone')" @click="cloneCp"
        v-show-if-allowed="cpResources.createOpts" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>
  </os-grid>
</template>

<script>

import routerSvc from '@/common/services/Router.js';

import cpResources from './Resources.js';

export default {
  props: ['cp'],

  inject: ['cpDict'],

  data() {
    return {
      ctx: {
        dict: []
      },

      cpResources
    };
  },

  created() {
    this.ctx.cp = this.cp;
    this.cpDict().then(dict => this.ctx.dict = dict);
  },

  computed: {
  },

  methods: {
    editCp: function() {
      routerSvc.goto('CpAddEdit', {cpId: this.cp.id});
    },

    cloneCp: function() {
      routerSvc.goto('CpAddEdit', {cpId: -1}, {copyOf: this.cp.id});
    }
  }
}
</script>
