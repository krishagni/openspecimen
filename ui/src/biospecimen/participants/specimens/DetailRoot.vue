
<template>
  <router-view :cpr="cpr" :visit="visit" :specimen="specimen" :key="specimen.id" v-if="specimen.id > 0" />
</template>

<script>

import {provide, ref, watch, toRefs} from 'vue';

import formUtil  from '@/common/services/FormUtil.js';
import spmnSvc  from '@/biospecimen/services/Specimen.js';

export default {
  props: ['cpr', 'visit', 'specimenId'],

  async setup(props) {
    const specimen = ref({});
    provide('specimen', specimen);
    specimen.value = {};

    const {specimenId} = toRefs(props);
    const loadSpecimen = async (id) => {
      specimen.value = await spmnSvc.getById(id);
      formUtil.createCustomFieldsMap(specimen.value, true);
    }

    loadSpecimen(specimenId.value);
    watch(
      specimenId,
      async (newVal, oldVal) => {
        if (newVal == oldVal || newVal == specimen.value.id) {
          return;
        }

        loadSpecimen(newVal);
      }
    );

    return { specimen };
  }
}
</script>
