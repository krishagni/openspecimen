
<template>
  <os-page-toolbar v-if="visit.sprName && !editMode">
    <template #default>
      <os-menu icon="download" :label="$t('common.buttons.download')" 
        :options="[
          {icon: 'file-alt', caption: $t('visits.text_report'), onSelect: () => this.downloadTextReport()},
          {icon: 'file-pdf', caption: $t('visits.pdf_report'),  onSelect: () => this.downloadPdfReport()},
        ]" v-if="hasTextReport" />

      <os-button left-icon="download" :label="$t('common.buttons.download')" @click="downloadReport" v-else />

      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editReport"
        v-if="hasTextReport && !visit.sprLocked" />

      <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteReport"
        v-if="!visit.sprLocked" />

      <os-button left-icon="lock"      :label="$t('common.buttons.lock')" @click="lockReport" v-if="!visit.sprLocked" />

      <os-button left-icon="lock-open" :label="$t('common.buttons.unlock')" @click="unlockReport" v-else />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8" v-if="!editMode">
      <div v-if="!visit.sprName">
        <os-message type="info">
          <span v-t="'visits.no_spr'"></span>
        </os-message>
        <os-button left-icon="upload" :label="$t('common.buttons.upload')" @click="showUploadDialog" />
      </div>
      <div class="report-text-wrapper" v-else>
        <div class="report-text" v-if="hasTextReport">
          <pre>{{textReport}}</pre>
        </div>
        <div v-else>
          <os-message type="info">
            <span v-t="'visits.non_text_spr'">Path report cannot be displayed, as it is not in text format.</span>
          </os-message>
        </div>
      </div>
    </os-grid-column>

    <os-grid-column width="8" v-else>
      <os-form class="report-text-editor" :schema="reportSchema" :data="formCtx">
        <os-button primary :label="$t('common.buttons.update')" @click="updateReport" />
        <os-button text    :label="$t('common.buttons.cancel')" @click="cancelEditReport" />
      </os-form>
    </os-grid-column>
  </os-grid>

  <os-dialog ref="uploadReportDialog">
    <template #header>
      <span v-t="'visits.upload_path_report'">Upload Path Report</span>
    </template>
    <template #content>
      <div>
        <os-label>
          <span v-t="'visits.choose_path_report_file'">Choose the path report file</span>
        </os-label>
        <os-file-upload ref="pathReportUploader" :url="uploadUrl" :auto="false" :headers="reqHeaders" />
      </div>
    </template>
    <template #footer>
      <os-button text    :label="$t('common.buttons.cancel')" @click="cancelUpload" />
      <os-button primary :label="$t('common.buttons.upload')" @click="uploadReport" />
    </template>
  </os-dialog>

  <os-confirm-delete ref="confirmDeleteReport" :captcha="false">
    <template #message>
      <span v-t="'visits.confirm_path_rpt_delete'">Are you sure you want to delete the path report?</span>
    </template>
  </os-confirm-delete>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import http      from '@/common/services/HttpClient.js';
import visitSvc  from '@/biospecimen/services/Visit.js';

export default {
  props: ['cpr', 'visit'],

  components: { },

  inject: ['cpViewCtx'],

  data() {
    return {
      hasTextReport: false,

      textReport: '',

      editMode: false,

      formCtx: {},

      reportSchema: {
        rows: [
          {
            fields: [
              {
                name: 'textReport',
                type: 'textarea',
                rows: 20,
                validations: {
                  required: {
                    messageCode: 'visits.report_text_required'
                  }
                }
              }
            ]
          }
        ]
      }
    }
  },

  async created() {
    this._loadSprText(); 
  },

  computed: {
    uploadUrl: function() {
      return visitSvc.getSprUploadUrl(this.visit);
    },

    reqHeaders: function() {
      return http.headers;
    }
  },

  methods: {
    showUploadDialog: function() {
      this.$refs.uploadReportDialog.open();
    },

    uploadReport: function() {
      this.$refs.pathReportUploader.upload().then(
        (sprName) => {
          alertsSvc.success({code: 'visits.path_report_uploaded'});
          Object.assign(this.visit, {sprName});
          this.cancelUpload();
          this._loadSprText();
        }
      );
    },

    cancelUpload: function() {
      this.$refs.uploadReportDialog.close();
    },

    editReport: function() {
      this.editMode = true;
      this.formCtx = {textReport: this.textReport};
    },

    cancelEditReport: function() {
      this.editMode = false;
    },

    updateReport: function() {
      const {textReport} = this.formCtx;
      visitSvc.updateSprText(this.visit, textReport).then(
        (savedTextReport) => {
          this.textReport = savedTextReport;
          alertsSvc.success({code: 'visits.path_text_report_saved'});
          this.cancelEditReport();
        }
      );
    },

    downloadTextReport: function() {
      visitSvc.downloadSpr(this.visit, 'text');
    },

    downloadPdfReport: function() {
      visitSvc.downloadSpr(this.visit, 'pdf');
    },

    downloadReport: function() {
      visitSvc.downloadSpr(this.visit);
    },

    lockReport: function() {
      visitSvc.lockSpr(this.visit).then(
        ({locked}) => {
          Object.assign(this.visit, {sprLocked: locked});
          alertsSvc.success({code: 'visits.path_report_locked'});
        }
      );
    },

    unlockReport: function() {
      visitSvc.unlockSpr(this.visit).then(
        ({locked}) => {
          Object.assign(this.visit, {sprLocked: locked});
          alertsSvc.success({code: 'visits.path_report_unlocked'});
        }
      );
    },

    deleteReport: function() {
      this.$refs.confirmDeleteReport.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          visitSvc.deleteSpr(this.visit).then(
            () => {
              Object.assign(this.visit, {sprName: null, sprLocked: false});
              this.hasTextReport = false;
              this.textReport = null;
              this.editMode = false;
              alertsSvc.success({code: 'visits.path_report_deleted'});
            }
          )
        }
      );
    },

    _loadSprText: function() {
      if (!this.visit.sprName) {
        return;
      }

      visitSvc.getSprText(this.visit).then(
        (text) => {
          this.hasTextReport = text && text.trim().length > 0
          this.textReport = text;
        }
      );
    }
  }
}
</script>

<style scoped>
.report-text-wrapper {
  height: 100%;
}

.report-text-wrapper .report-text {
  height: calc(100% - 1rem);
  overflow-y: scroll;
  padding: 0rem 1rem;
  border: 1px solid #ddd;
  border-radius: 0.25rem
}

.report-text-editor :deep(.os-input-text textarea) {
  font-family: monospace;
}
</style>
