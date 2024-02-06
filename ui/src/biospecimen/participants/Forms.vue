<template>
  <os-page-toolbar>
    <template #default>
      <span>Action buttons</span>
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
        @rowClicked="onRecordClick">
        <template #expansionRow>
          <FormRecordOverview :record="record" v-if="record" />
        </template>
      </os-list-view>
    </os-grid-column>
  </os-grid>
</template>

<script>
import cprSvc from '@/biospecimen/services/Cpr.js';
import formSvc from '@/forms/services/Form.js';
import routerSvc from '@/common/services/Router.js';
import util   from '@/common/services/Util.js';

import FormRecordOverview from '@/forms/components/FormRecordOverview.vue';

export default {
  props: ['cpr', 'formId', 'recordId'],

  inject: ['cpViewCtx'],

  components: {
    FormRecordOverview
  },

  data() {
    return {
      loading: true,

      forms: [],

      records: [],

      expanded: []
    }
  },

  async created() {
    const promises = [];
    promises.push(cprSvc.getForms(this.cpr));
    promises.push(cprSvc.getFormRecords(this.cpr));
    if (this.formId > 0 && this.recordId > 0) {
      promises.push(formSvc.getRecord({formId: this.formId, recordId: this.recordId}, {includeMetadata: true}));
    }

    Promise.all(promises).then(
      ([forms, records, record]) => {
        this.forms = forms;
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
