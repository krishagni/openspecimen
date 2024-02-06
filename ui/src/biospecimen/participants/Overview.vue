<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="edit" />

      <os-button left-icon="user-secret" :label="$t('participants.anonymize')" @click="anonymize" />

      <os-button left-icon="print" :label="$t('common.buttons.print')" @click="printLabels" />

      <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteCpr" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />

      <os-section v-if="hasOccurredVisits">
        <template #title>
          <span v-t="'participants.occurred_visits'">Visits</span>
        </template>

        <template #content>
          <OccurredVisits :cp="ctx.cp" :cpr="ctx.cpr" :visits="ctx.visits" :dict="ctx.visitDict" />
        </template>
      </os-section>

      <os-section v-if="hasMissedVisits">
        <template #title>
          <span v-t="'participants.missed_or_not_collected_visits'">Visits</span>
        </template>

        <template #content>
          <MissedVisits :cp="ctx.cp" :cpr="ctx.cpr" :visits="ctx.visits" :dict="ctx.visitDict" />
        </template>
      </os-section>

      <os-section v-if="hasPendingVisits">
        <template #title>
          <span v-t="'participants.pending_visits'">Visits</span>
        </template>

        <template #content>
          <PendingVisits :cp="ctx.cp" :cpr="ctx.cpr" :visits="ctx.visits" :dict="ctx.visitDict" />
          <!-- Visits :cp="ctx.cp" :cpr="ctx.cpr" :visits="ctx.visits" :dict="ctx.visitDict" / -->
        </template>
      </os-section>
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.auditObjs" v-if="ctx.cpr.id" />
    </os-grid-column>

    <os-confirm ref="confirmAnonymizeDialog">
      <template #title>
        <span v-t="'participants.erase_phi_data'">Erase Participant PHI Data</span>
      </template>
      <template #message>
        <span v-t="'participants.erase_phi_data_q'">Are you sure you want to erase all PHI data of participant?</span>
      </template>
    </os-confirm>

    <os-delete-object ref="deleteCprDialog" :input="ctx.deleteOpts" />
  </os-grid>
</template>

<script>
import alertSvc from '@/common/services/Alerts.js';
import formUtil from '@/common/services/FormUtil.js';
import routerSvc from '@/common/services/Router.js';
import util from '@/common/services/Util.js';

import cprSvc from '@/biospecimen/services/Cpr.js';
import visitSvc from '@/biospecimen/services/Visit.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';

import MissedVisits   from './MissedVisits.vue';
import OccurredVisits from './OccurredVisits.vue';
import PendingVisits  from './PendingVisits.vue';
// import Visits from './Visits.vue';

export default {
  props: ['cpr'],

  components: {
    MissedVisits,
    OccurredVisits,
    PendingVisits
//     Visits
  },

  inject: ['cpViewCtx'],

  data() {
    return {
      ctx: {
        cp: {},

        cpr: {},

        dict: [],

        auditObjs: [],

        visits: [],

        routeQuery: this.$route.query,

        visitDict: undefined
      }
    };
  },

  async created() {
    this._setupCpr();
    this.ctx.dict = await this.cpViewCtx.getCprDict();
    this.ctx.cp = await this.cpViewCtx.getCp();
    this.ctx.visitDict = await this.cpViewCtx.getVisitDict();
  },

  computed: {
    hasOccurredVisits: function() {
      return (this.ctx.visits || []).some(visit => visit.status == 'Complete');
    },

    hasMissedVisits: function() {
      return (this.ctx.visits || []).some(visit => visit.status == 'Missed Collection' || visit.status == 'Not Collected');
    },

    hasPendingVisits: function() {
      return (this.ctx.visits || []).some(visit => !visit.status || visit.status == 'Pending');
    }
  },

  watch: {
    cpr: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupCpr();
      }
    }
  },

  methods: {
    edit: function() {
      const cpr = this.ctx.cpr;
      routerSvc.goto('ParticipantAddEdit', {cpId: cpr.cpId, cprId: cpr.id});
    },

    anonymize: function() {
      this.$refs.confirmAnonymizeDialog.open().then(
        (resp) => {
          if (resp != 'proceed') {
            return;
          }

          cprSvc.anonymize(this.cpr).then(
            () => {
              // TODO: Is there a better way to do this reload?
              routerSvc.reload();
              alertSvc.success({code: 'participants.anonymized_successfully'});
            }
          );
        }
      );
    },

    printLabels: function() {
      const cpr = this.ctx.cpr;
      const ts = util.formatDate(new Date(), 'yyyyMMdd_HHmmss');
      const outputFilename = [cpr.cpShortTitle, cpr.ppid, ts].join('_') + '.csv';
      specimenSvc.printLabels({cprId: cpr.id}, outputFilename);
    },

    deleteCpr: function() {
      this.$refs.deleteCprDialog.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('ParticipantsList', {cprId: -2}, this.ctx.routeQuery);
          }
        }
      );
    },

    _setupCpr: function() {
      const cpr = this.ctx.cpr = this.cpr;

      this.ctx.auditObjs = [
        {objectId: cpr.id, objectName: 'collection_protocol_registration'},
        {objectId: cpr.participant.id, objectName: 'participant'}
      ]

      let name = '';
      if (cpr.participant.firstName && cpr.participant.firstName != '###') {
        name = cpr.participant.firstName;
      }

      if (cpr.participant.lastName && cpr.participant.lastName != '###') {
        if (name) {
          name += ' ';
        }

        name += cpr.participant.lastName;
      }

      name = cpr.ppid + (name ? ' (' + name + ')' : '');
      this.ctx.deleteOpts = {
        type: this.$t('participants.cpr'),
        title: name,
        dependents: () => cprSvc.getDependents(cpr),
        forceDelete: true,
        askReason: true,
        deleteObj: (reason) => cprSvc.deleteCpr(cpr.id, true, reason)
      };

      const eventRulesQ = this.cpViewCtx.getAnticipatedEventsRules();
      const visitsQ = visitSvc.getVisits(this.cpr);
      Promise.all([eventRulesQ, visitsQ]).then(
        ([eventRules, visits]) => {
          const allowedEvents = cprSvc.getAllowedEvents(this.ctx.cpr, eventRules);
          this.ctx.visits = (visits || [])
            .filter(
              visit => {
                if (visit.status && visit.status != 'Pending') {
                  return true;
                }

                return !allowedEvents || !visit.eventCode || allowedEvents.indexOf(visit.eventCode) != -1;
              }
            );

          for (let visit of this.ctx.visits) {
            formUtil.createCustomFieldsMap(visit, true);
          }
        }
      );
    }
  }
}
</script>
