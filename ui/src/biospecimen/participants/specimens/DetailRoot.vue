
<template>
  <router-view :cpr="cpr" :visit="visit" :specimen="specimen" :key="specimen.id" v-if="specimen.id > 0" />
</template>

<script>

import {provide, ref} from 'vue';

import formUtil  from '@/common/services/FormUtil.js';
import spmnSvc  from '@/biospecimen/services/Specimen.js';

export default {
  props: ['cpr', 'visit', 'specimenId'],

  async setup(props) {
    const specimen = ref({});
    provide('specimen', specimen);

    specimen.value = await spmnSvc.getById(props.specimenId);
    formUtil.createCustomFieldsMap(specimen.value, true);
    return { specimen };
  },

  watch: {
    specimenId: async function(newVal, oldVal) {
      if (newVal == oldVal || newVal == this.specimen.id) {
        return;
      }

      this.specimen = await spmnSvc.getById(newVal);
      formUtil.createCustomFieldsMap(this.specimen, true);
    }
  }
}
</script>
