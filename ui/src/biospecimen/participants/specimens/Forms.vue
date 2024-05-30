<template>
  <os-forms-list :object="specimen" :api="api" :formId="formId" :recordId="recordId" />
</template>

<script>
import routerSvc from '@/common/services/Router.js';
import specimenSvc  from '@/biospecimen/services/Specimen.js';

export default {
  props: ['cpr', 'visit', 'specimen', 'formId', 'recordId'],

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
      getPage          : () => 'specimen-forms',
      getView          : () => 'record-actions',
      getViewProps     : () => ({cp: this.cpViewCtx.getCp(), cpr: this.cpr, visit: this.visit, specimen: this.specimen})
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
      const ctxt = {cp, cpr: this.cpr, visit: this.visit, specimen: this.specimen};
      return this.cpViewCtx.getSpecimenForms(ctxt);
    },

    _getSurveyForms: async function() {
      return [];
    },

    _getFormRecords: async function() {
      return specimenSvc.getFormRecords(this.specimen);
    },

    _addEditFormRecord: function(formId, formCtxtId, recordId) {
      const {cpId, cprId, visitId, eventId, id: specimenId} = this.specimen;
      routerSvc.goto('SpecimenAddEditFormRecord', {cpId, cprId, visitId, specimenId}, {formId, formCtxtId, recordId, eventId});
    },

    _isUpdateAllowed: function() {
      const vc = this.cpViewCtx;
      const {lineage} = this.specimen;
      return lineage == 'New' ? vc.isUpdateSpecimenAllowed(this.cpr) : vc.isUpdateAllSpecimenAllowed(this.cpr);
    },
  }
}
</script>
