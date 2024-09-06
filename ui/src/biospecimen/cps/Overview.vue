<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editCp"
        v-show-if-allowed="cpResources.updateOpts" />

      <os-button left-icon="check" :label="$t('cps.publish')" @click="showPublishCpDialog"
        v-show-if-allowed="cpResources.updateOpts" v-if="cp.draftMode && cp.activityStatus == 'Active'" />

      <os-button left-icon="copy" :label="$t('common.buttons.clone')" @click="cloneCp"
        v-show-if-allowed="cpResources.createOpts" />

      <os-button left-icon="ban" :label="$t('common.buttons.close')" @click="closeCp"
        v-show-if-allowed="cpResources.updateOpts" v-if="cp.activityStatus == 'Active'" />

      <os-button left-icon="check" :label="$t('common.buttons.reopen')" @click="reopenCp"
        v-show-if-allowed="cpResources.updateOpts" v-if="cp.activityStatus == 'Closed'" />

      <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteCp"
        v-show-if-allowed="cpResources.deleteOpts" />
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

    <os-confirm ref="closeCpConfirmDialog">
      <template #title>
        <span v-t="'cps.close_cp_q'">Close Collection Protocol?</span>
      </template>
      <template #message>
        <span v-t="'cps.confirm_close_cp_q'">New participants cannot be registered to the closed collection protocols. Are you sure you want to proceed with closing the collection protocol?</span>
      </template>
    </os-confirm>

    <os-delete-object ref="deleteCpDialog" :input="ctx.deleteOpts" />
  </os-grid>
</template>

<script>

import alertSvc from '@/common/services/Alerts.js';
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';
import util from '@/common/services/Util.js';

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
    },

    closeCp: function() {
      this.$refs.closeCpConfirmDialog.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          const toSave = util.clone(this.cp);
          toSave.activityStatus = 'Closed';
          cpSvc.saveOrUpdate(toSave).then(
            () => {
              this.$emit('cp-saved');
              alertSvc.success({code: 'cps.cp_closed', args: this.cp});
            }
          );
        }
      );
    },

    reopenCp: function() {
      const toSave = util.clone(this.cp);
      toSave.activityStatus = 'Active';
      cpSvc.saveOrUpdate(toSave).then(
        () => {
          this.$emit('cp-saved');
          alertSvc.success({code: 'cps.cp_reopened', args: this.cp});
        }
      );
    },

    deleteCp: function() {
      this.ctx.deleteOpts = {
        type: this.$t('cps.cp'),
        title: this.cp.shortTitle,
        dependents: () => cpSvc.getDependents(this.cp),
        forceDelete: true,
        askReason: true,
        deleteObj: (reason) => cpSvc.deleteCp(this.cp.id, true, reason)
      }
      setTimeout(
        async () => {
          const resp = await this.$refs.deleteCpDialog.execute();
          if (resp == 'deleted') {
            routerSvc.goto('CpsList', {cpId: -1});
          } else if (resp == 'in_progress') {
            alertSvc.info({code: 'cps.cp_delete_in_progress', args: this.cp});
            routerSvc.goto('CpsList', {cpId: -1});
          }
        }
      );
    }
  }
}
</script>
