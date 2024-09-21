
<template>
  <router-view :cpr="cpr" :key="cpr.id || -1" />
</template>

<script>

import {provide, ref} from 'vue';

import formUtil from '@/common/services/FormUtil.js';
import cprSvc   from '@/biospecimen/services/Cpr.js';

export default {
  props: ['cprId'],

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
    cprId: async function(newVal, oldVal) {
      if (newVal == oldVal || newVal == this.cpr.id) {
        return;
      }

      this._setupCpr();
    }
  },

  methods: {
    _setupCpr: async function() {
      if (!this.cprId || this.cprId <= 0) {
        this.cpr = {participant: {pmis: [], source: 'OpenSpecimen'}};
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
