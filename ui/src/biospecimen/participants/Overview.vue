<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="user-secret" :label="$t('participants.anonymize')" @click="anonymize" />

      <os-button left-icon="print" :label="$t('common.buttons.print')" @click="printLabels" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />

      <os-section v-if="occurredVisits.length > 0">
        <template #title>
          <span v-t="'participants.occurred_visits'">Occurred Visits</span>
        </template>

        <template #content>
          <OccurredVisits :visits="occurredVisits" />
        </template>
      </os-section>

      <os-section v-if="missedVisits.length > 0">
        <template #title>
          <span v-t="'participants.missed_or_not_collected_visits'">Missed/Not Collected Visits</span>
        </template>

        <template #content>
          <MissedVisits :visits="missedVisits" />
        </template>
      </os-section>

      <os-section v-if="pendingVisits.length > 0">
        <template #title>
          <span v-t="'participants.pending_visits'">Pending Visits</span>
        </template>

        <template #content>
          <PendingVisits :cpr="ctx.cpr" :visits="pendingVisits" />
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

import MissedVisits from './MissedVisits.vue';
import PendingVisits from './PendingVisits.vue';
import OccurredVisits from './OccurredVisits.vue';

export default {
  props: ['cpr'],

  components: {
    MissedVisits,
    PendingVisits,
    OccurredVisits
  },

  inject: ['cpViewCtx'],

  data() {
    return {
      ctx: {
        cpr: {},

        dict: [],

        auditObjs: [],

        visits: [],

        routeQuery: this.$route.query
      }
    };
  },

  async created() {
    this._setupCpr();
    this.ctx.dict = await this.cpViewCtx.value.getCprDict();
  },

  computed: {
    occurredVisits: function() {
      return (this.ctx.visits || []).filter(visit => visit.status == 'Complete');
    },

    missedVisits: function() {
      return (this.ctx.visits || []).filter(visit => ['Not Collected', 'Missed Collection'].indexOf(visit.status) >= 0);
    },

    pendingVisits: function() {
      return (this.ctx.visits || []).filter(visit => !visit.status || visit.status == 'Pending');
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

    _setupCpr: function() {
      const cpr = this.ctx.cpr = this.cpr;
      this.ctx.auditObjs = [
        {objectId: cpr.id, objectName: 'collection_protocol_registration'},
        {objectId: cpr.participant.id, objectName: 'participant'}
      ]

      visitSvc.getVisits(this.cpr).then(
        visits => {
          this.ctx.visits = visits
          for (let visit of visits) {
            formUtil.createCustomFieldsMap(visit, true);
          }
        }
      );
    }
  }
}
</script>
