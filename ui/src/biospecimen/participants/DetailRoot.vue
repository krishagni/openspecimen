
<template>
  <router-view :cpr="cpr" :key="key" v-if="(!cprId || cprId <= 0) || (cpr && cpr.id > 0)" />
</template>

<script>

import {provide, ref} from 'vue';

import formUtil from '@/common/services/FormUtil.js';
import cprSvc   from '@/biospecimen/services/Cpr.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cprId', 'op'],

  inject: ['cpViewCtx'],

  setup() {
    const cpr = ref({participant: {pmis: [], source: 'OpenSpecimen'}});
    provide('cpr', cpr);
    return { cpr };
  },

  created() {
    this._setupCpr();
  },

  watch: {
    key: async function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this._setupCpr();
    }
  },

  computed: {
    key: function() {
      return this.cprId + '_' + (this.op || 'view');
    }
  },

  methods: {
    _setupCpr: async function() {
      if (!this.cprId || this.cprId <= 0) {
        this.cpr = {participant: {pmis: [], source: 'OpenSpecimen'}};

        const cp = this.cpViewCtx.getCp();
        routerSvc.goto('ParticipantsList', {cpId: cp.id});
        return;
      }

      this.cpr = await cprSvc.getCpr(this.cprId);
      formUtil.createCustomFieldsMap(this.cpr.participant, true);

      if (this.$osSvc.ecValidationSvc) {
        const cp = this.cpViewCtx.getCp();
        if (cp.visitLevelConsents) {
          this.cpr.hasConsented = true;
        } else {
          const {status} = await this.$osSvc.ecValidationSvc.getParticipantStatus(this.cprId);
          this.cpr.hasConsented = status;
        }
      } else {
        this.cpr.hasConsented = true;
      }
    }
  }
}
</script>
