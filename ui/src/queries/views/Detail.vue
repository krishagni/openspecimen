<template>
  <router-view :query="query" :key="query.id" v-if="query" />
</template>

<script>
import savedQuerySvc from '../services/SavedQuery.js';

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
