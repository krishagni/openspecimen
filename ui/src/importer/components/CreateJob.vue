<template>
  <div>
    <os-form ref="importForm" :schema="jobFs" :data="ctx" @input="handleInput">
      <template v-slot:[`job.inputFileId`]>
        <a class="template-url" :href="templateUrl">
          <span v-t="'import.download_template'">Download template</span>
        </a>
      </template>

      <template #default v-if="$slots.default">
        <slot></slot>
      </template>
    </os-form>

    <os-confirm ref="confirmTxnPerRecordDialog">
      <template #title>
        <span v-t="'import.txn_exceeded'">Pre-validate Records Limit Exceeded</span>
      </template>
      <template #message>
        <span v-t="{path: 'import.confirm_txn_per_record', args: ctx}"></span>
      </template>
    </os-confirm>
  </div>
</template>

<script>
import addJobSchema from '../schemas/create-job.js';

import alertsSvc   from '@/common/services/Alerts.js';
import http        from '@/common/services/HttpClient.js';
import jobSvc      from '@/importer/services/ImportJob.js';
import settingsSvc from '@/common/services/Setting.js';
import util        from '@/common/services/Util.js';

export default {
  props: [ 'object-type', 'object-params', 'hide-ops', 'show-upsert', 'record-types', 'csv-type',  'entities-label', 'entities'],
  
  data() {
    const {global: {locale}} = this.$ui;
    return {
      ctx: {
        job: {
          objectType: this.objectType,

          dateFormat: locale.shortDateFmt,

          timeFormat: locale.timeFmt,

          atomic: true
        },

        recordTypes: [],

        hideOps: this.hideOps,

        getImportTypes: this._getImportTypes,

        getRecordTypes: this._getRecordTypes
      }
    }
  },

  async created() {
    this._setupRecordTypes();
  },

  computed: {
    templateUrl: function() {
      const params = {};
      if (this.objectParams) {
        Object.assign(params, this.objectParams);
      }

      const {job: {fieldSeparator, entityId}} = this.ctx;
      if (fieldSeparator) {
        params.fieldSeparator = fieldSeparator;
      }

      if (entityId) {
        params.entityId = entityId;
      }

      const {recordType} = this.ctx.job;
      if (recordType) {
        params.schema = recordType.type;
        Object.assign(params, recordType.params || {});
      } else {
        params.schema = this.objectType;
      }

      return http.getUrl('import-jobs/input-file-template', {query: params});
    },

    jobFs: function() {
      let {layout} = addJobSchema;
      if (this.entitiesLabel && typeof this.entities == 'function') {
        const row = {
          fields: [
            {
              type: "dropdown",
              name: "job.entityId",
              labelCode: this.entitiesLabel,
              listSource: {
                selectProp: 'id',
                displayProp: 'title',
                loadFn: ({query}) => this.entities(query)
              }
            }
          ]
        };

        layout = util.clone(layout);
        layout.rows.unshift(row);
      }

      return layout;
    }
  },

  watch: {
    recordTypes: async function() {
      this._setupRecordTypes();
    }
  },

  methods: {
    importRecords: async function() {
      if (!this.$refs.importForm.validate()) {
        return null;
      }

      const payload = util.clone(this.ctx.job);
      payload.inputFileId  = payload.inputFileId && payload.inputFileId.fileId;
      payload.objectParams = this.objectParams || {};
      payload.csvType      = this.csvType;

      if (+payload.entityId > 0) {
        payload.objectParams.entityId = payload.entityId;
        delete payload.entityId;
      }

      const {recordType} = payload;
      if (recordType) {
        payload.objectType   = recordType.type;
        payload.objectParams = Object.assign(payload.objectParams || {}, recordType.params || {});
        delete payload.recordType;
      }

      return jobSvc.createJob(payload).then(
        (savedJob) => {
          if (savedJob.status == 'TXN_SIZE_EXCEEDED') {
            return this._handleTxnSizeExceeded(savedJob);
          }

          alertsSvc.success({code: 'import.job_created', args: savedJob});
          return savedJob;
        }
      );
    },

    _setupRecordTypes: async function() {
      let recordTypes = [];
      if (typeof this.recordTypes == 'function') {
        recordTypes = await this.recordTypes();
      }

      this.ctx.recordTypes = recordTypes || [];
    },

    _getRecordTypes: async function() {
      return this.ctx.recordTypes || [];
    },

    _getImportTypes: function() {
      const types = [
        { captionCode: 'import.create', value: 'CREATE' },
        { captionCode: 'import.update', value: 'UPDATE' }
      ];

      if (this.showUpsert) {
        types.push({ captionCode: 'import.upsert', value: 'UPSERT' });
      }

      return types;
    },

    _handleTxnSizeExceeded: async function(job) {
      const [setting] = await settingsSvc.getSetting('common', 'import_max_records_per_txn');
      Object.assign(this.ctx, {totalRecords: job.totalRecords, maxTxnSize: setting.value});

      const resp = await this.$refs.confirmTxnPerRecordDialog.open();
      if (resp == 'proceed') {
        this.ctx.job.atomic = false;
        return this.importRecords();
      }

      return null;
    }
  }
}
</script>

<style scoped>
.template-url {
  display: inline-block;
  margin: 0.5rem 0rem;
}
</style>
