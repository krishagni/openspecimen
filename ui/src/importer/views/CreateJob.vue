<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb v-if="bcrumb">
        <os-breadcrumb :items="bcrumb" />
      </template>

      <h3>{{pageTitle}}</h3>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column :width="12">
          <os-import-records ref="impRecsForm" :object-type="objectType" :object-params="objectParams" v-bind="$attrs">
            <os-button primary :label="$t('import.validate_n_import')" @click="submitJob" />

            <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
          </os-import-records>
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['bcrumb', 'title', 'object-type', 'object-params', 'on-submit'],

  computed: {
    pageTitle: function() {
      if (typeof this.title == 'function') {
        return this.title(this.objectType, this.objectParams);
      }

      return this.$t(this.title);
    }
  },

  methods: {
    submitJob: async function() {
      const job = await this.$refs.impRecsForm.importRecords();
      if (job == null) {
        return null;
      }

      if (typeof this.onSubmit == 'function') {
        this.onSubmit(job);
      } else {
        routerSvc.back();
      }
    },

    cancel: function() {
      routerSvc.back();
    }
  },
}
</script>
