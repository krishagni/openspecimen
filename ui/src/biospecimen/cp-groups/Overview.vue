<template>
  <os-page-toolbar v-if="updateAllowed || importAllowed || exportAllowed">
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="gotoEdit" v-if="updateAllowed" />

      <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteCpg" v-if="updateAllowed" />

      <os-menu icon="tasks" :label="$t('cpgs.workflows')" :options="wfOpts" v-if="wfOpts.length > 0" />

      <os-menu icon="download" :label="$t('common.buttons.export')" :options="exportOpts" v-if="exportAllowed" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-list-view :data="cpsList" :schema="cpsListSchema" :show-row-actions="true"
        :loading="ctx.loading" @rowClicked="onCpRowClick">
        <template #rowActions="{rowObject}">
          <os-button-group>
            <os-button left-icon="user-friends" v-os-tooltip.bottom="$t('cps.view_participants')"
              @click="viewParticipants(rowObject.cp)" v-if="!rowObject.cp.specimenCentric" />
            <os-button left-icon="flask" v-os-tooltip.bottom="$t('cps.view_specimens')"
              @click="viewSpecimens(rowObject.cp)" />
            <os-button left-icon="table" v-os-tooltip.bottom="$t('cps.view_catalog')"
              @click="viewCatalog(rowObject.cp)" v-if="rowObject.cp.catalogId > 0" />
          </os-button-group>
        </template>

        <template #footer>
          <os-pager :start-at="ctx.startAt" :have-more="ctx.haveMoreCps"
            @previous="previousPage" @next="nextPage" />
        </template>
      </os-list-view>
    </os-grid-column>
  </os-grid>

  <os-confirm ref="confirmDeleteDialog">
    <template #title>
      <span v-t="'cpgs.delete_cpg_q'">Delete Collection Protocol Group?</span>
    </template>
    <template #message>
      <span v-t="{path: 'cpgs.delete_cpg', args: cpg}">Delete Collection Protocol Group xyz?</span>
    </template>
  </os-confirm>

  <os-dialog ref="importWfDialog">
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
      <os-button text    :label="$t('common.buttons.cancel')" @click="hideImportWfDialog" />
      <os-button primary :label="$t('common.buttons.import')" @click="importWorkflows" />
    </template>
  </os-dialog>
</template>

<script>
import alertSvc  from '@/common/services/Alerts.js';
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import cpgSvc    from '@/biospecimen/services/CollectionProtocolGroup.js';
import http      from '@/common/services/HttpClient.js';
import exportSvc from '@/common/services/ExportService.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cpg', 'permOpts'],

  data() {
    return {
      ctx: {
        cps: [],

        startAt: 0,

        haveMoreCps: false,

        loading: false,

        pageSize: 1
      }
    }
  },

  created() {
    if (this.cpg && this.cpg.id > 0) {
      this._loadCps(0);
    }
  },

  watch: {
    cpg: function(newVal, oldVal) {
      if (!newVal || (oldVal && newVal.id == oldVal.id)) {
        return;
      }

      this._loadCps(0);
    }
  },

  computed: {
    importAllowed: function() {
      return this.permOpts && this.permOpts.importAllowed;
    },

    exportAllowed: function() {
      return this.permOpts && this.permOpts.exportAllowed;
    },

    updateAllowed: function() {
      return this.permOpts && this.permOpts.updateAllowed;
    },

    wfOpts: function() {
      const opts = [];
      if (this.exportAllowed) {
        opts.push({icon: 'download', caption: this.$t('common.buttons.export'), onSelect: this.exportWorkflows});
      }

      if (this.importAllowed) {
        opts.push({icon: 'upload',   caption: this.$t('common.buttons.import'), onSelect: this.showImportWfDialog});
      }

      return opts;
    },

    exportOpts: function() {
      return [
        {icon: 'calendar', caption: this.$t('cpgs.collection_protocols'),  onSelect: this.exportCps},
        {icon: 'list-alt', caption: this.$t('cpgs.cpes'),                  onSelect: this.exportCpEvents},
        {icon: 'flask',    caption: this.$t('cpgs.specimen_requirements'), onSelect: this.exportCpSrs}
      ];
    },

    cpsList: function() {
      return (this.ctx.cps || []).map(cp => ({cp}));
    },

    cpsListSchema: function() {
      return {
        columns: [
          {
            name: "cp.shortTitle",
            captionCode: "cps.title",
            href: (row) => routerSvc.getUrl('CpDetail.Overview', {cpId: row.rowObject.cp.id}),
            value: ({cp}) => {
              let result = cp.shortTitle;
              if (cp.code) {
                result += ' (' + cp.code + ')';
              }

              return result;
            }
          },
          {
            name: "cp.principalInvestigator",
            captionCode: "cps.pi",
            type: "user"
          },
          {
            name: "cp.startDate",
            captionCode: "cps.start_date",
            type: "date"
          }
        ]
      };
    },

    workflowsUploadUrl: function() {
      return http.getUrl('collection-protocol-groups/' + this.cpg.id + '/workflows-file');
    },

    reqHeaders: function() {
      return http.headers;
    },
  },

  methods: {
    previousPage: function() {
      this._loadCps(this.ctx.startAt - this.ctx.pageSize);
    },

    nextPage: function() {
      this._loadCps(this.ctx.startAt + this.ctx.pageSize);
    },

    gotoEdit: function() {
      routerSvc.goto('CpgAddEdit', {cpgId: this.cpg.id});
    },

    deleteCpg: function() {
      this.$refs.confirmDeleteDialog.open().then(
        resp => {
          if (resp == 'proceed') {
            cpgSvc.deleteCpg(this.cpg).then(
              () => {
                alertSvc.success({code: 'cpgs.deleted', args: this.cpg});
                routerSvc.goto('CpgsList', {cpgId: -2});
              }
            );
          }
        }
      );
    },

    showImportWfDialog: function() {
      this.$refs.importWfDialog.open();
    },

    hideImportWfDialog: function() {
      this.$refs.importWfDialog.close();
    },

    importWorkflows: function() {
      this.$refs.wfJsonUploader.upload().then(
        () => {
          alertSvc.success({code: 'cpgs.workflows_imported'});
          this.hideImportWfDialog()
        }
      );
    },

    exportWorkflows: function() {
      cpgSvc.exportWorkflows(this.cpg);
    },

    exportCps: function() {
      this._exportCpRecords('cp');
    },

    exportCpEvents: function() {
      this._exportCpRecords('cpe');
    },

    exportCpSrs: function() {
      this._exportCpRecords('sr');
    },

    onCpRowClick: function({cp}) {
      routerSvc.goto('CpDetail.Overview', {cpId: cp.id});
    },

    viewParticipants: function(cp) {
      routerSvc.goto('ParticipantsList', {cpId: cp.id});
    },

    viewSpecimens: function(cp) {
      routerSvc.goto('ParticipantsList', {cpId: cp.id}, {view: 'specimens_list'});
    },

    viewCatalog: function(cp) {
      routerSvc.goto('CatalogSearch', {catalogId: cp.catalogId}, {cpId: cp.id});
    },

    _loadCps: async function(startAt) {
      if (!this.cpg || !this.cpg.id) {
        return;
      }

      const ctx = this.ctx;
      const pageSize = ctx.pageSize;
      ctx.loading = true;

      const cps = await cpSvc.getCps({groupId: this.cpg.id, startAt, maxResults: pageSize + 1});
      if (cps.length > pageSize) {
        ctx.haveMoreCps = true;
        cps.splice(cps.length - 1, 1);
      } else {
        ctx.haveMoreCps = false;
      }

      ctx.cps = cps;
      ctx.startAt = startAt;
      ctx.loading = false;
    },

    _exportCpRecords: async function(objectType) {
      const {id: groupId} = this.cpg;
      exportSvc.exportRecords({objectType, params: {groupId}});
    }
  }
}
</script>
