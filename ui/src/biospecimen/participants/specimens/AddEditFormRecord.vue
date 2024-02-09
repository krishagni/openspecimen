<template>
  <os-addedit-form-record-view :api="api" :object="specimen"
    :form-id="formId" :form-ctxt-id="formCtxtId" :record-id="recordId" />
</template>

<script>
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cpr', 'visit', 'specimen', 'formId', 'formCtxtId', 'recordId'],

  inject: ['cpViewCtx'],

  data() {
    return {
      api: {},

      ctx: {
        cp: {},

        formDef: {}
      }
    };
  },

  async created() {
    this.api = {
      getBreadcrumb: this._getBreadcrumb,

      getForms: this._getForms,

      gotoOverview: this._gotoOverview
    };
  },

  methods: {
    _getBreadcrumb: function() {
      const {cpId, cprId, visitId, eventId, id: specimenId} = this.specimen;
      return [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}),
          label: this.visit.cpShortTitle
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId}),
          label: this.cpr.ppid
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId, eventId}),
          label: cpSvc.getEventDescription(this.visit)
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemSpecimenDetail.Overview', {cpId, cprId, visitId, eventId, specimenId}),
          label: this.specimen.label
        }
      ];
    },

    _getForms: async function() {
      return this.cpViewCtx.getCp().then(
        cp => this.cpViewCtx.getSpecimenForms({cp: cp, cpr: this.cpr, visit: this.visit, specimen: this.specimen})
      );
    },

    _gotoOverview: function(formId, recordId) {
      const {cpId, cprId, visitId, id: specimenId} = this.specimen;
      if (specimenId > 0) {
        routerSvc.goto('ParticipantsListItemSpecimenDetail.Forms', {cpId, cprId, visitId, specimenId}, {formId, recordId});
      } else {
        routerSvc.goto('ParticipantsList', {cpId, cprId: -1});
      }
    }
  }
}
</script>
