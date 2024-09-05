<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editCp"
        v-show-if-allowed="cpResources.updateOpts" />

      <os-button left-icon="check" :label="$t('cps.publish')" @click="showPublishCpDialog"
        v-show-if-allowed="cpResources.updateOpts" v-if="cp.draftMode" />

      <os-button left-icon="copy" :label="$t('common.buttons.clone')" @click="cloneCp"
        v-show-if-allowed="cpResources.createOpts" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />
    </os-grid-column>

    <os-dialog ref="publishCpDialog">
      <template #header>
        <span v-t="{path: 'cps.publish_cp', args: cp}">Publish CP</span>
      </template>
      <template #content>
        <os-form ref="publishForm" :schema="ctx.publishSchema" :data="ctx" />
      </template>
      <template #footer>
        <os-button primary :label="$t('cps.publish')" @click="publishCp" />
        <os-button text :label="$t('common.buttons.cancel')" @click="hidePublishCpDialog" />
      </template>
    </os-dialog>
  </os-grid>
</template>

<script>

import alertSvc from '@/common/services/Alerts.js';
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';

import cpResources from './Resources.js';

export default {
  props: ['cp'],

  inject: ['cpDict'],

  emits: ['cp-saved'],

  data() {
    return {
      ctx: {
        dict: [],

        publishSchema: {rows: []}
      },

      cpResources
    };
  },

  created() {
    this.ctx.cp = this.cp;
    this.cpDict().then(dict => this.ctx.dict = dict);
    this.ctx.publishSchema = cpSvc.getPublishFormSchema();
  },

  computed: {
  },

  methods: {
    editCp: function() {
      routerSvc.goto('CpAddEdit', {cpId: this.cp.id});
    },

    showPublishCpDialog: function() {
      this.ctx.publish = {};
      this.$refs.publishCpDialog.open();
    },

    publishCp: function() {
      if (!this.$refs.publishForm.validate()) {
        return;
      }

      cpSvc.publishCp(this.cp.id, this.ctx.publish).then(
        () => {
          this.$emit('cp-saved');
          this.hidePublishCpDialog();
          alertSvc.success({code: 'cps.cp_published', args: this.cp});
        }
      );
    },

    hidePublishCpDialog: function() {
      this.$refs.publishCpDialog.close();
    },

    cloneCp: function() {
      routerSvc.goto('CpAddEdit', {cpId: -1}, {copyOf: this.cp.id});
    }
  }
}
</script>
