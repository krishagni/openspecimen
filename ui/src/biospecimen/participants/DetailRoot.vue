
<template>
  <router-view :cpr="cpr" :key="cpr.id || -1" />
</template>

<script>

import {provide, ref} from 'vue';

import formUtil from '@/common/services/FormUtil.js';
import cprSvc   from '@/biospecimen/services/Cpr.js';

export default {
  props: ['cprId'],

  async setup(props) {
    const cpr = ref({});
    provide('cpr', cpr);

    if (props.cprId > 0) {
      cpr.value = await cprSvc.getCpr(props.cprId);
      formUtil.createCustomFieldsMap(cpr.value.participant, true);
    } else {
      cpr.value = {participant: {pmis: [], source: 'OpenSpecimen'}}
    }
    
    return { cpr };
  },

  watch: {
    cprId: async function(newVal, oldVal) {
      if (newVal == oldVal || newVal == this.cpr.id) {
        return;
      }

      this.cpr = await cprSvc.getCpr(newVal);
      formUtil.createCustomFieldsMap(this.cpr.participant, true);
    }
  }
}
</script>
