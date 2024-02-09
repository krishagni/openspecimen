<template>
  <template v-if="!hasEc">
    <ConsentResponsesList :cp="cp" :cpr="cpr" />
  </template>

  <os-plugin-views page="participant-consents" view="list" :viewProps="{cp, cpr}" />
</template>

<script>
import ConsentResponsesList from '@/biospecimen/consents/ConsentResponsesList';

export default {
  props: ['cpr'],

  inject: ['cpViewCtx'],

  components: {
    ConsentResponsesList
  },

  data() {
    return {
      cp: {}
    }
  },

  created() {
    this.cpViewCtx.getCp().then(cp => this.cp = cp);
  },

  computed: {
    hasEc: function() {
      return this.$osSvc.ecDocSvc != null;
    }
  }
}
</script>
