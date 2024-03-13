<template>
  <template v-if="!hasEc">
    <ConsentResponsesList :cp="cp" :cpr="cpr" :accessFns="accessFns" />
  </template>

  <os-plugin-views page="participant-consents" view="list" :viewProps="{cp, cpr}" />
</template>

<script>
import ConsentResponsesList from '@/biospecimen/consents/ResponsesList';

export default {
  props: ['cpr'],

  inject: ['cpViewCtx'],

  components: {
    ConsentResponsesList
  },

  data() {
    return {
      cp: this.cpViewCtx.getCp()
    }
  },

  computed: {
    hasEc: function() {
      return this.$osSvc.ecDocSvc != null;
    },

    accessFns: function() {
      return {
        isUpdateAllowed: () => this.cpViewCtx.isUpdateConsentAllowed(this.cpr),

        isDeleteAllowed: () => this.cpViewCtx.isDeleteConsentAllowed(this.cpr)
      };
    }
  }
}
</script>
