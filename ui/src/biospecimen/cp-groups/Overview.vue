<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="gotoEdit" v-if="updateAllowed" />

      <os-menu icon="tasks" :label="$t('cpgs.workflows')" :options="wfOpts" v-if="eximAllowed" />

      <os-menu icon="download" :label="$t('common.buttons.export')" :options="exportOpts" v-if="eximAllowed" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-list-view :data="cpsList" :schema="cpsListSchema" :show-row-actions="true" @rowClicked="onCpRowClick">
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
      </os-list-view>
    </os-grid-column>
  </os-grid>

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
import cpgSvc    from '@/biospecimen/services/CollectionProtocolGroup.js';
import http      from '@/common/services/HttpClient.js';
import exportSvc from '@/common/services/ExportService.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cpg', 'permOpts'],

  data() {
    return {
      ctx: {
      }
    }
  },

  computed: {
    eximAllowed: function() {
      return this.permOpts && this.permOpts.eximAllowed;
    },

    updateAllowed: function() {
      return this.permOpts && this.permOpts.updateAllowed;
    },

    wfOpts: function() {
      return [
        {icon: 'download', caption: this.$t('common.buttons.export'), onSelect: this.exportWorkflows},
        {icon: 'upload',   caption: this.$t('common.buttons.import'), onSelect: this.showImportWfDialog}
      ]
    },

    exportOpts: function() {
      return [
        {icon: 'calendar', caption: this.$t('cpgs.collection_protocols'),  onSelect: this.exportCps},
        {icon: 'list-alt', caption: this.$t('cpgs.cpes'),                  onSelect: this.exportCpEvents},
        {icon: 'flask',    caption: this.$t('cpgs.specimen_requirements'), onSelect: this.exportCpSrs}
      ];
    },

    cpsList: function() {
      return (this.cpg.cps || [])
        .sort((c1, c2) => c1.shortTitle < c2.shortTitle ? -1 : (c1.shortTitle > c2.shortTitle ? 1 : 0))
        .map(cp => ({cp}));
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
    gotoEdit: function() {
      routerSvc.goto('CpgAddEdit', {cpgId: this.cpg.id});
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

    _exportCpRecords: function(objectType) {
      exportSvc.exportRecords({objectType, recordIds: this.cpg.cps.map(cp => cp.id)});
    }
  }
}
</script>
