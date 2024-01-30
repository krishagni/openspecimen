
<template>
  <router-view :cpr="cpr" :visit="visit" :specimen="specimen" :key="viewKey" v-if="viewKey && loaded" />
</template>

<script>

import {provide, ref} from 'vue';

import formUtil  from '@/common/services/FormUtil.js';
import spmnSvc  from '@/biospecimen/services/Specimen.js';

export default {
  props: ['cpr', 'visit', 'specimenId'],

  async setup() {
    const specimen = ref({});
    provide('specimen', specimen);

    const loaded = ref(false);
    return { specimen, loaded };
  },

  created() {
    if (this.specimenId > 0) {
      this._loadSpecimen(this.specimenId);
    }
  },

  computed: {
    viewKey: function() {
      return 's-' + (this.specimenId > 0 ? this.specimenId : 'unknown');
    }
  },

  watch: {
    '$route.params.specimenId': function(newVal, oldVal) {
      if (newVal == oldVal || newVal == this.specimen.id) {
        return;
      }

      this._loadSpecimen(newVal);
    }
  },

  methods: {
    _loadSpecimen: async function(specimenId) {
      this.specimen = await spmnSvc.getById(specimenId);
      formUtil.createCustomFieldsMap(this.specimen, true);
      this.loaded = true;
    }
  }
}
</script>
