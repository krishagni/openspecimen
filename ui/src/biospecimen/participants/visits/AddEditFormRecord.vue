<template>
  <os-addedit-form-record-view :api="api" :object="visit"
    :form-id="formId" :form-ctxt-id="formCtxtId" :record-id="recordId" v-if="api" />
</template>

<script>
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cpr', 'visit', 'formId', 'formCtxtId', 'recordId'],

  inject: ['cpViewCtx'],

  data() {
    const cp = this.cpViewCtx.getCp();
    return {
      api: {
        getBreadcrumb: this._getBreadcrumb,

        getForms: this._getForms,

        gotoOverview: this._gotoOverview,

        isDraftDataEntryEnabled: () => cp.draftDataEntry
      },

      ctx: {
        cp,

        formDef: {}
      }
    };
  },

  methods: {
    _getBreadcrumb: function() {
      const {cpId, cprId, id: visitId, eventId} = this.visit;
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
        }
      ];
    },

    _getForms: async function() {
      return this.cpViewCtx.getVisitForms({cp: this.ctx.cp, cpr: this.cpr, visit: this.visit})
    },

    _gotoOverview: function(formId, recordId) {
      const {cpId, cprId, id: visitId, eventId} = this.visit;
      if (visitId > 0) {
        routerSvc.goto('ParticipantsListItemVisitDetail.Forms', {cpId, cprId, visitId, eventId}, {formId, recordId});
      } else {
        routerSvc.goto('ParticipantsList', {cpId, cprId: -1});
      }
    }
  }
}
</script>
