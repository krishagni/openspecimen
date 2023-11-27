<template>
  <os-page-toolbar>
    <template #default>
      <span> Action buttons </span>
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
  </os-grid>
</template>

<script>
import formUtil from '@/common/services/FormUtil.js';
import visitSvc from '@/biospecimen/services/Visit.js';

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
