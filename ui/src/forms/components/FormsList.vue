<template>
  <os-page-toolbar v-if="isUpdateAllowed && (formsList.length > 0 || surveys.length > 0)">
    <template #default>
      <os-menu left-icon="plus" :label="$t('common.buttons.add')" :options="formsList"
        v-if="formsList.length > 0" />

      <os-menu left-icon="plus" :label="$t('forms.survey_mode')" :options="surveys"
        v-if="surveys.length > 0" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-message info v-if="loading">
        <span>Loading form records. Please wait for a moment...</span>
      </os-message>

      <os-list-view v-else
        class="os-list-shadowed-rows"
        :data="records"
        :schema="{columns: recordFields}"
        :expanded="expanded"
        :showRowActions="true"
        :noRecordsMsg="noRecordsMsg"
        @rowClicked="onRecordClick">

        <template #rowActions="{rowObject}" v-if="isUpdateAllowed">
          <os-button-group v-if="!rowObject.sysForm">
            <os-button left-icon="edit" v-os-tooltip="$t('common.buttons.edit')" @click="editRecord(rowObject)" />

            <os-button left-icon="trash" v-os-tooltip="$t('common.buttons.delete')" @click="deleteRecord(rowObject)" />
          </os-button-group>
        </template>

        <template #expansionRow>
          <FormRecordOverview :record="record" v-if="record" />
        </template>
      </os-list-view>
    </os-grid-column>

    <DeleteFormRecord ref="deleteFormDialog" />

    <os-edc-confirm-survey-mode ref="selectSurveyMode" :object="object" />
  </os-grid>
</template>

<script>
import formSvc from '@/forms/services/Form.js';
import routerSvc from '@/common/services/Router.js';
import util   from '@/common/services/Util.js';

import DeleteFormRecord   from '@/forms/components/DeleteFormRecord.vue';
import FormRecordOverview from '@/forms/components/FormRecordOverview.vue';

export default {
  props: ['object', 'api', 'formId', 'recordId'],

  components: {
    DeleteFormRecord,
    FormRecordOverview
  },

  data() {
    return {
      loading: true,

      forms: undefined,

      surveys: [],

      records: [],

      expanded: []
    }
  },

  async created() {
    const promises = [];
    promises.push(this.api.getFormRecords());
    if (this.formId > 0 && this.recordId > 0) {
      promises.push(formSvc.getRecord({formId: this.formId, recordId: this.recordId}, {includeMetadata: true}));
    }

    Promise.all(promises).then(
      ([records, record]) => {
        this.records = records;
        this.record = record;
        if (this.formId > 0 && this.recordId > 0) {
          const selected = records.find(({formId, recordId}) => formId == this.formId && recordId == this.recordId);
          this.expanded = [selected];
        }

        this.loading = false;
      }
    );

    this._loadForms();
  },

  computed: {
    formsList: function() {
      return (this.forms || []).filter(form => !form.sysForm && (form.noOfRecords == 0 || form.multiRecord))
        .map(form => ({caption: form.formCaption, onSelect: () => this.addRecord(form)}));
    },

    recordFields: function() {
      return [
        {
          name: 'recordName',
          label: this.$t('common.record'),
          value: function({recordId, formCaption, formStatus}) {
            return '#' + recordId + ' ' + formCaption + (formStatus == 'DRAFT' ? ' (Draft)' : '')
          }
        },
        {
          name: 'user',
          label: this.$t('common.updated_by'),
          type: 'user'
        },
        {
          name: 'updateTime',
          label: this.$t('common.update_time'),
          type: 'date-time'
        }
      ]
    },

    isUpdateAllowed: function() {
      return !this.api || !this.api.isUpdateAllowed || this.api.isUpdateAllowed()
    },

    noRecordsMsg: function() {
      return this.isUpdateAllowed ? 'common.no_form_records' : 'common.no_form_records_no_add'
    }
  },

  watch: {
    '$route.query': function() {
      this._loadRecord();
    }
  },

  methods: {
    onRecordClick: function({formId, recordId}) {
      let {name, params, query} = routerSvc.getCurrentRoute();
      query  = util.clone(query || {});

      const [selected] = this.expanded;
      if (selected && selected.formId == formId && selected.recordId == recordId) {
        this.expanded.length = 0;
        routerSvc.goto(name, params, Object.assign(query, {formId: undefined, recordId: undefined}));
        return;
      }

      routerSvc.goto(name, params, Object.assign(query, {formId, recordId}));
    },

    addRecord: function({formId, formCtxtId}) {
      this.api.addEditFormRecord(formId, formCtxtId);
    },

    editRecord: function({formId, fcId: formCtxtId, recordId}) {
      this.api.addEditFormRecord(formId, formCtxtId, recordId);
    },

    deleteRecord: function(record) {
      this.$refs.deleteFormDialog.execute(record).then(
        () => {
          const idx = this.records.indexOf(record);
          this.records.splice(idx, 1);
          if (this.forms) {
            const form = this.forms.find(form => form.formCtxtId == record.fcId);
            form.noOfRecords -= 1;
          }
        }
      );
    },

    switchToSurveyMode: function(survey) {
      this.$refs.selectSurveyMode.gotoSurvey(survey);
    },

    _loadForms: function() {
      if (this.forms) {
        return;
      }

      const promises = [
        this.api.getForms(),
        this.api.getSurveyForms()
      ];

      Promise.all(promises).then(
        ([forms, surveys]) => {
          this.forms = forms;
          this.surveys = surveys.filter(
            survey => {
              for (let form of forms) {
                if (form.entityType == survey.entityType && form.formCtxtId == survey.formCtxtId) {
                  return !form.sysForm && (form.noOfRecords == 0 || form.multiRecord);
                }
              }

              return false;
            }
          ).map(
            survey => ({caption: survey.formCaption, onSelect: () => this.switchToSurveyMode(survey)})
          );
        }
      );
    },

    _loadRecord: function() {
      if (this.formId > 0 && this.recordId > 0) {
        formSvc.getRecord({formId: this.formId, recordId: this.recordId}, {includeMetadata: true}).then(
          (record) => {
            const selected = this.records.find(({formId, recordId}) => formId == this.formId && recordId == this.recordId);
            this.expanded = [selected];
            this.record = record;
          }
        );
      } else {
        this.expanded.length = 0;
        this.record = null;
      }
    }
  }
}
</script>
