<template>
  <os-addedit-form-record-view :api="api" :object="cpr"
    :form-id="formId" :form-ctxt-id="formCtxtId" :record-id="recordId" v-if="api" />
</template>

<script>
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cpr', 'formId', 'formCtxtId', 'recordId'],

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
      const {cpId, id: cprId} = this.cpr;
      return [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}),
          label: this.cpr.cpShortTitle
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId}),
          label: this.cpr.ppid
        }
      ];
    },

    _getForms: async function() {
      return this.cpViewCtx.getParticipantForms({cp: this.ctx.cp, cpr: this.cpr})
    },

    _gotoOverview: function(formId, recordId) {
      const {cpId, id: cprId} = this.cpr;
      if (cprId > 0) {
        routerSvc.goto('ParticipantsListItemDetail.Forms', {cpId, cprId}, {formId, recordId});
      } else {
        routerSvc.goto('ParticipantsList', {cpId, cprId: -1});
      }
    }
  }
}
</script>
