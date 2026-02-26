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
          <span>: </span>
          <span v-show="recordId == 'SpecimenCollectionEvent'" v-t="'specimens.collection_event'" />
          <span v-show="recordId == 'SpecimenReceivedEvent'" v-t="'specimens.received_event'" />
          <span v-show="+formId > 0">{{ctx.formDef.caption}}</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="eventForm" :schema="eventSchema" :data="eventCtx"
        v-if="recordId == 'SpecimenCollectionEvent' || recordId == 'SpecimenReceivedEvent'">
        <div>
          <os-button primary :label="$t('common.buttons.update')" @click="updateSpecimen" />
          <os-button text :label="$t('common.buttons.cancel')" @click="eventCancelled" />
        </div>
      </os-form>

      <os-addedit-form-record :entity="specimen" :form-def="ctx.formDef" :form-id="formId"
        :form-ctxt-id="formCtxtId" :record-id="recordId" :hide-panel="true"
        @saved="eventSaved" @cancelled="eventCancelled" v-else />
    </os-page-body>
  </os-page>
</template>

<script>

import alertsSvc  from '@/common/services/Alerts.js';
import cpSvc      from '@/biospecimen/services/CollectionProtocol.js';
import formSvc    from '@/forms/services/Form.js';
import routerSvc  from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';
import spmnSvc    from '@/biospecimen/services/Specimen.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['cpr', 'visit', 'specimen', 'formId', 'formCtxtId', 'recordId'],

  inject: ['cpViewCtx'],

  data() {
    const cp = this.cpViewCtx.getCp();
    return {
      ctx: {
        cp,

        formDef: {}
      },

      eventCtx: { wasReceived: false, allowSpmnRelabeling: false }
    };
  },

  async created() {
    if (+this.formId > 0) {
      this.ctx.formDef = await formSvc.getDefinition(this.formId);
    }

    const {receivedEvent: re} = this.specimen || {};
    const settings = await settingSvc.getSetting('administrative', 'allow_spmn_relabeling');
    this.eventCtx = {
      wasReceived: re && re.receivedQuality && re.receivedQuality != 'To be Received',
      allowSpmnRelabeling: util.isTrue(settings[0].value),
      specimen: util.clone(this.specimen)
    };
  },

  computed: {
    bcrumb: function() {
      const cp = this.ctx.cp;
      if (!cp) {
        return [];
      }

      const {cpId, cprId, visitId, eventId, id: specimenId} = this.specimen;
      const bcrumb = [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}),
          label: cp.shortTitle
        }
      ];

      if (!cp.specimenCentric) {
        bcrumb.push(
          {
            url: routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId}),
            label: this.cpr.ppid
          }
        );

        bcrumb.push(
          {
            url: routerSvc.getUrl('ParticipantsListItemVisitDetail.Overview', {cpId, cprId, visitId, eventId}),
            label: cpSvc.getEventDescription(this.visit)
          }
        );
      }

      bcrumb.push(
        {
          url: routerSvc.getUrl('ParticipantsListItemSpecimenDetail.Overview', {cpId, cprId, visitId, eventId, specimenId}),
          label: this.specimen.label
        }
      );

      return bcrumb;
    },

    eventSchema: function() {
      if (this.recordId == 'SpecimenCollectionEvent') {
        return spmnSvc.getCollectionEventAddEditFs();
      } else if (this.recordId == 'SpecimenReceivedEvent') {
        const {wasReceived, allowSpmnRelabeling} = this.ctx;
        return spmnSvc.getReceivedEventAddEditFs(!wasReceived && allowSpmnRelabeling);
      }

      return {rows: []};
    }
  },

  methods: {
    updateSpecimen: function() {
      if (!this.$refs.eventForm.validate()) {
        return;
      }

      const {id, collectionEvent, receivedEvent} = util.clone(this.eventCtx.specimen);

      let msgKey = '';
      let toSave = null;
      if (this.recordId == 'SpecimenCollectionEvent') {
        msgKey = 'specimens.coll_event_saved';
        toSave = {id, collectionEvent};
      } else {
        msgKey = 'specimens.recv_event_saved';
        toSave = {id, receivedEvent};
      }

      spmnSvc.saveOrUpdate(toSave).then(
        saved => {
          alertsSvc.success({code: msgKey});
          this._navToOverview(saved);
        }
      );
    },

    eventSaved: function() {
      alertsSvc.success({code: 'specimens.event_saved', args: {eventName: this.ctx.formDef.caption}});
      this._navToOverview(this.specimen);
    },

    eventCancelled: function() {
      this._navToOverview(this.specimen);
    },

    _navToOverview: function({cpId, cprId, visitId, id: specimenId}) {
      if (specimenId > 0) {
        routerSvc.goto('ParticipantsListItemSpecimenDetail.Overview', {cpId, cprId, visitId, specimenId});
      } else {
        routerSvc.back();
      }
    }
  }
}
</script>
