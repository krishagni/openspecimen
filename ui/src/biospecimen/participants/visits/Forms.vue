<template>
  <os-forms-list :object="visit" :api="api" :formId="formId" :recordId="recordId" />
</template>

<script>
import routerSvc from '@/common/services/Router.js';
import visitSvc  from '@/biospecimen/services/Visit.js';

export default {
  props: ['cpr', 'visit', 'formId', 'recordId'],

  inject: ['cpViewCtx'],

  data() {
    return {
      api: { },
    }
  },

  async created() {
    this.api = {
      getForms         : this._getForms,
      getSurveyForms   : this._getSurveyForms,
      getFormRecords   : this._getFormRecords,
      addEditFormRecord: this._addEditFormRecord
    }
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

  methods: {
    _getForms: async function() {
      return this.cpViewCtx.getCp().then(
        cp => {
          const ctxt = {cp: cp, cpr: this.cpr, visit: this.visit};
          return this.cpViewCtx.getVisitForms(ctxt);
        }
      );
    },

    _getSurveyForms: async function() {
      return [];
    },

    _getFormRecords: async function() {
      return visitSvc.getFormRecords(this.visit);
    },

    _addEditFormRecord: function(formId, formCtxtId, recordId) {
      const {cpId, cprId, id: visitId, eventId} = this.visit;
      routerSvc.goto('VisitAddEditFormRecord', {cpId, cprId, visitId}, {formId, formCtxtId, recordId, eventId});
    }
  }
}
</script>
