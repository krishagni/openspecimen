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
      addEditFormRecord: this._addEditFormRecord,
      isUpdateAllowed  : this._isUpdateAllowed,
      getPage          : () => 'visit-forms',
      getView          : () => 'record-actions',
      getViewProps     : () => ({cp: this.cpViewCtx.getCp(), cpr: this.cpr, visit: this.visit})
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
      const cp = this.cpViewCtx.getCp();
      const userRole = this.cpViewCtx.getRole();

      const ctxt = {cp, cpr: this.cpr, visit: this.visit, userRole};
      return this.cpViewCtx.getVisitForms(ctxt);
    },

    _getSurveyForms: async function() {
      return this.cpViewCtx.getSurveyForms();
    },

    _getFormRecords: async function() {
      return visitSvc.getFormRecords(this.visit);
    },

    _addEditFormRecord: function(formId, formCtxtId, recordId) {
      const {cpId, cprId, id: visitId, eventId} = this.visit;
      routerSvc.goto('VisitAddEditFormRecord', {cpId, cprId, visitId}, {formId, formCtxtId, recordId, eventId});
    },

    _isUpdateAllowed: function() {
      return this.cpViewCtx.isUpdateVisitAllowed(this.cpr);
    }
  }
}
</script>
