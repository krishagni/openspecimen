<template>
  <os-addedit-form-record-view :api="api" :object="specimen"
    :form-id="formId" :form-ctxt-id="formCtxtId" :record-id="recordId" v-if="api" />
</template>

<script>
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cpr', 'visit', 'specimen', 'formId', 'formCtxtId', 'recordId'],

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
      const {cpId, cprId, visitId, eventId, id: specimenId} = this.specimen;
      const bcrumb = [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}),
          label: this.visit.cpShortTitle
        }
      ];

      const cp = this.cpViewCtx.getCp();
      if (!cp.specimenCentric) {
        bcrumb.push({
          url: routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId}),
          label: this.cpr.ppid
        });

        bcrumb.push({
          url: routerSvc.getUrl('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId, eventId}),
          label: cpSvc.getEventDescription(this.visit)
        });
      }

      bcrumb.push({
        url: routerSvc.getUrl('ParticipantsListItemSpecimenDetail.Overview', {cpId, cprId, visitId, eventId, specimenId}),
        label: this.specimen.label
      });
      return bcrumb;
    },

    _getForms: async function() {
      return this.cpViewCtx.getSpecimenForms({cp: this.ctx.cp, cpr: this.cpr, visit: this.visit, specimen: this.specimen})
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
