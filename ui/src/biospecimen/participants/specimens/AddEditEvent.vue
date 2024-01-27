<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span>
        <h3>
          <span v-show="!recordId" v-t="'specimens.add_event'">Add Event</span>
          <span v-show="!!recordId" v-t="'specimens.update_event'">Update Event</span>
          <span>: {{ctx.formDef.caption}}</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-addedit-form-record :entity="specimen" :form-def="ctx.formDef" :form-id="formId"
        :form-ctxt-id="formCtxtId" :record-id="recordId" :hide-panel="true"
        @saved="eventSaved" @cancelled="eventCancelled" />
    </os-page-body>
  </os-page>
</template>

<script>

import alertsSvc  from '@/common/services/Alerts.js';
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import formSvc    from '@/forms/services/Form.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cpr', 'visit', 'specimen', 'formId', 'formCtxtId', 'recordId'],

  inject: ['cpViewCtx'],

  data() {
    this.cpViewCtx.getCp().then(cp => this.ctx.cp = cp);
    return {
      ctx: {
        cp: {},

        formDef: {}
      }
    };
  },

  async created() {
    this.ctx.formDef = await formSvc.getDefinition(this.formId);
  },

  computed: {
    bcrumb: function() {
      const cp = this.ctx.cp;
      if (!cp) {
        return [];
      }

      const {cpId, cprId, visitId, eventId, id: specimenId} = this.specimen;
      return [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}),
          label: cp.shortTitle
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
    }
  },

  methods: {
    eventSaved: function() {
      alertsSvc.success({code: 'specimens.event_saved', args: {eventName: this.ctx.formDef.caption}});
      this._navToOverview(this.specimen);
    },

    eventCancelled: function() {
      this._navToOverview(this.specimen);
    },

    _navToOverview: function({cpId, cprId, visitId, id: specimenId}) {
      if (id > 0) {
        routerSvc.goto('ParticipantsListItemSpecimenDetail.Overview', {cpId, cprId, visitId, specimenId});
      } else {
        routerSvc.back();
      }
    }
  }
}
</script>
