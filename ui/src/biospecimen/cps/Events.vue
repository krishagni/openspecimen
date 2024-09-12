<template>
  <router-view :cp="cp" v-if="cp && cp.id > 0" :key="cp.id"> </router-view>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cp'],

  mounted() {
    this._gotoEventsList();
  },

  watch: {
    '$route': function() {
      this._gotoEventsList();
    }
  },

  methods: {
    _gotoEventsList: function() {
      const {name} = routerSvc.getCurrentRoute();
      if (name == 'CpDetail.Events') {
        routerSvc.goto('CpDetail.Events.List');
      }
    }
  }
}
</script>
