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

      <os-button left-icon="users" :label="$t('cps.view_participants')" @click="viewParticipants"
        v-if="!cp.specimenCentric" />

      <os-button left-icon="flask" :label="$t('cps.view_specimens')" @click="viewSpecimens"
        v-if="cp.specimenCentric" />

      <os-menu :label="$t('common.buttons.more')" :options="ctx.moreOptions" />
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

    <os-dialog ref="importWorkflowsDialog">
      <template #header>
        <span v-t="'cps.import_workflows'">Import Workflows JSON</span>
      </template>
      <template #content>
        <div>
          <os-label>
            <span v-t="'cps.choose_workflows_json_file'">Choose the workflows JSON file</span>
          </os-label>
          <os-file-upload ref="wfJsonUploader" :url="workflowsUploadUrl" :auto="false" :headers="reqHeaders" />
        </div>
      </template>
      <template #footer>
        <os-button text    :label="$t('common.buttons.cancel')" @click="hideImportWorkflowsDialog" />
        <os-button primary :label="$t('common.buttons.import')" @click="importWorkflows" />
      </template>
    </os-dialog>
  </os-grid>
</template>

<script>

import alertSvc from '@/common/services/Alerts.js';
import authSvc from '@/common/services/Authorization.js';
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import http  from '@/common/services/HttpClient.js';
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

        publishSchema: {rows: []},

        moreOptions: []
      },

      cpResources
    };
  },

  created() {
    this.ctx.cp = this.cp;
    this.cpDict().then(dict => this.ctx.dict = dict);
    this.ctx.publishSchema = cpSvc.getPublishFormSchema();
    this._loadMoreMenuOptions();
  },

  computed: {
    workflowsUploadUrl: function() {
      return http.getUrl('collection-protocols/' + this.cp.id + '/workflows-file');
    },

    reqHeaders: function() {
      return http.headers;
    },
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

      cpSvc.publish(this.cp.id, this.ctx.publish).then(
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
    },

    viewParticipants: function() {
      routerSvc.goto('ParticipantsList', {cprId: -1});
    },

    viewSpecimens: function() {
      routerSvc.goto('ParticipantsList', {cprId: -1}, {view: 'specimens_list'});
    },

    exportCpJson: function() {
      cpSvc.exportJson(this.cp);
    },

    exportCpCsv: function() {
      cpSvc.exportCsv(this.cp);
    },

    exportCpEvents: function() {
      cpSvc.exportEvents(this.cp);
    },

    exportCpSrs: function() {
      cpSvc.exportSpecimenReqs(this.cp);
    },

    exportWorkflows: function() {
      cpSvc.exportWorkflows(this.cp);
    },

    showImportWorkflowsDialog: function() {
      this.$refs.importWorkflowsDialog.open();
    },

    hideImportWorkflowsDialog: function() {
      this.$refs.importWorkflowsDialog.close();
    },

    importWorkflows: function() {
      this.$refs.wfJsonUploader.upload().then(
        () => {
          alertSvc.success({code: 'cps.workflows_imported', args: this.cp});
          this.hideImportWorkflowsDialog()
        }
      );
    },

    _loadMoreMenuOptions: function() {
      const moreOptions = [];
      if (authSvc.isAllowed(cpResources.importOpts)) {
        moreOptions.push({icon: 'calendar', caption: this.$t('cps.export_json'),   onSelect: this.exportCpJson});
        moreOptions.push({icon: 'calendar', caption: this.$t('cps.export_csv'),    onSelect: this.exportCpCsv});
        moreOptions.push({icon: 'list-alt', caption: this.$t('cps.export_events'), onSelect: this.exportCpEvents});
        moreOptions.push({icon: 'flask',    caption: this.$t('cps.export_srs'),    onSelect: this.exportCpSrs});
      }

      if (authSvc.isAllowed(cpResources.updateOpts)) {
        if (moreOptions.length > 0) {
          moreOptions.push({divider: true});
        }

        moreOptions.push({icon: 'file-code', caption: this.$t('cps.export_workflows'), onSelect: this.exportWorkflows});
        moreOptions.push({icon: 'file-code', caption: this.$t('cps.import_workflows'), onSelect: this.showImportWorkflowsDialog});
      }

      this.ctx.moreOptions = moreOptions;
    }
  }
}
</script>
