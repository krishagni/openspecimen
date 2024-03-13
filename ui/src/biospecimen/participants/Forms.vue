<template>
  <os-forms-list :object="cpr" :api="api" :formId="formId" :recordId="recordId" />
</template>

<script>
import cprSvc  from '@/biospecimen/services/Cpr.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cpr', 'formId', 'recordId'],

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
      ];
    }
  },

  methods: {
    _getForms: async function() {
      const cp = this.cpViewCtx.getCp();
      const ctxt = {cp, cpr: this.cpr};
      return this.cpViewCtx.getParticipantForms(ctxt);
    },

    _getSurveyForms: async function() {
      return this.cpViewCtx.getSurveyForms();
    },

    _getFormRecords: async function() {
      return cprSvc.getFormRecords(this.cpr);
    },

    _addEditFormRecord: function(formId, formCtxtId, recordId) {
      const {cpId, id: cprId} = this.cpr;
      routerSvc.goto('ParticipantAddEditFormRecord', {cpId, cprId}, {formId, formCtxtId, recordId});
    },

    _isUpdateAllowed: function() {
      return this.cpViewCtx.isUpdateParticipantAllowed(this.cpr);
    }
  }
}
</script>
