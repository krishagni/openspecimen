
<template>
  <router-view :cpr="cpr" :visit="visit" :key="viewKey" v-if="viewKey" />
</template>

<script>

import {provide, ref} from 'vue';

import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import visitSvc  from '@/biospecimen/services/Visit.js';

import formUtil  from '@/common/services/FormUtil.js';

export default {
  props: ['cpr', 'visitId', 'eventId'],

  async setup() {
    const visit = ref({});
    provide('visit', visit);
    return { visit };
  },

  created() {
    if (this.visitId > 0) {
      this._loadVisit(this.visitId);
    } else if (this.eventId > 0) {
      this._loadEvent(this.eventId);
    }
  },

  computed: {
    viewKey: function() {
      if (this.visitId > 0) {
        return 'v-' + this.visitId;
      } else if (this.eventId > 0) {
        return 'e-' + this.cpr.cpId + '-' + this.eventId;
      }

      return '';
    }
  },

  watch: {
    visitId: function(newVal, oldVal) {
      if (newVal == oldVal || newVal == this.visit.id) {
        return;
      }

      this._loadVisit(newVal);
    },

    eventId: function(newVal, oldVal) {
      if (this.visitId > 0 || newVal == oldVal || newVal == this.visit.eventId) {
        return;
      }

      this._loadEvent(newVal);
    }
  },

  methods: {
    _loadVisit: function(visitId) {
      visitSvc.getVisit(visitId).then(
        visit => {
          this.visit = visit;
          formUtil.createCustomFieldsMap(this.visit, true);
        }
      );
    },

    _loadEvent: function(eventId) {
      cpSvc.getCpe(eventId).then(
        event => {
          event.eventCode = event.code;
          event.eventId = event.id;
          if (event.clinicalDiagnosis) {
            event.clinicalDiagnoses = [event.clinicalDiagnosis];
          }

          if (event.defaultSite) {
            event.site = event.defaultSite
          }

          delete event.id;
          this.visit = event;
        }
      );
    }
  }
}
</script>
