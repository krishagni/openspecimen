<template>
  <os-page-toolbar>
    <template #default>
      <os-menu left-icon="plus" :label="$t('common.buttons.add')" :options="participantForms"
        @menu-toggled="loadForms" v-if="!this.forms || participantForms.length > 0" />
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
        :noRecordsMsg="'common.no_form_records'"
        @rowClicked="onRecordClick">

        <template #rowActions="{rowObject}">
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
  </os-grid>
</template>

<script>
import cprSvc from '@/biospecimen/services/Cpr.js';
import formSvc from '@/forms/services/Form.js';
import routerSvc from '@/common/services/Router.js';
import util   from '@/common/services/Util.js';

import DeleteFormRecord   from '@/forms/components/DeleteFormRecord.vue';
import FormRecordOverview from '@/forms/components/FormRecordOverview.vue';

export default {
  props: ['cpr', 'formId', 'recordId'],

  inject: ['cpViewCtx'],

  components: {
    DeleteFormRecord,
    FormRecordOverview
  },

  data() {
    return {
      loading: true,

      forms: undefined,

      records: [],

      expanded: []
    }
  },

  async created() {
    const promises = [];
    promises.push(cprSvc.getFormRecords(this.cpr));
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
  },

  computed: {
    participantForms: function() {
      return (this.forms || []).filter(form => !form.sysForm && (form.noOfRecords == 0 || form.multiRecord))
        .map(form => ({caption: form.formCaption, onSelect: () => this.addRecord(form)}));
    },

    recordFields: function() {
      return [
        {
          name: 'recordName',
          label: this.$t('common.record'),
          value: function({recordId, formCaption}) {
            return '#' + recordId + ' ' + formCaption
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
      const {cpId, id: cprId} = this.cpr;
      routerSvc.goto('ParticipantAddEditFormRecord', {cpId, cprId}, {formId, formCtxtId});
    },

    editRecord: function({formId, fcId: formCtxtId, recordId}) {
      const {cpId, id: cprId} = this.cpr;
      routerSvc.goto('ParticipantAddEditFormRecord', {cpId, cprId}, {formId, formCtxtId, recordId});
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

    loadForms: function() {
      if (this.forms) {
        return;
      }

      this.cpViewCtx.getCp().then(
        cp => {
          const ctxt = {cp: cp, cpr: this.cpr};
          this.cpViewCtx.getParticipantForms(ctxt).then(forms => this.forms = forms);
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
