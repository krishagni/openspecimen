<template>
  <router-view :query="query" :key="query.id" v-if="query" @query-saved="reloadQuery" />
</template>

<script>

import routerSvc from '@/common/services/Router.js';
import savedQuerySvc from '@/queries/services/SavedQuery.js';

export default {
  props: ['queryId'],

  data() {
    return {
      query: null
    }
  },

  created() {
    this._loadQuery();
  },

  watch: {
    queryId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._loadQuery();
      }
    }
  },

  methods: {
    reloadQuery: function(query) {
      const {name} = routerSvc.getCurrentRoute();
      this.query = query;

      routerSvc.goto(name, {queryId: query.id});
    },

    _loadQuery: function() {
      if (!this.queryId || this.queryId == -1) {
        this.query = {};
        return;
      }

      savedQuerySvc.getQueryById(this.queryId).then(query => this.query = query);
    }
  }
}
</script>
