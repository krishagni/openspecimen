
<template>
  <router-view :cpr="cpr" :visit="visit" :key="visit.id" v-if="visit.id > 0" />
</template>

<script>

import {provide, ref} from 'vue';

import formUtil  from '@/common/services/FormUtil.js';
import visitSvc  from '@/biospecimen/services/Visit.js';

export default {
  props: ['cpr', 'visitId'],

  async setup(props) {
    const visit = ref({});
    provide('visit', visit);

    visit.value = await visitSvc.getVisit(props.visitId);
    formUtil.createCustomFieldsMap(visit.value, true);
    return { visit };
  },

  watch: {
    visitId: async function(newVal, oldVal) {
      if (newVal == oldVal || newVal == this.visit.id) {
        return;
      }

      this.visit = await visitSvc.getVisit(newVal);
      formUtil.createCustomFieldsMap(this.visit, true);
    }
  }
}
</script>
