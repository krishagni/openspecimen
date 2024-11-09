<template>
  <router-view :query="query" :key="query.id" v-if="query" @query-saved="reloadQuery" />
</template>

<script>

import formCache from '@/queries/services/FormsCache.js';
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
    reloadQuery: async function(query) {
      const {name} = routerSvc.getCurrentRoute();
      if (this.queryId == query.id || ((!this.queryId || this.queryId < 0) && !query.id)) {
        await this._hydrateFilters(query);
        this.query = query;
      } else {
        routerSvc.goto(name, {queryId: query.id});
      }
    },

    _loadQuery: async function() {
      if (!this.queryId || this.queryId == -1) {
        this.query = {queryExpression: [], filters: [], selectList: []};
        return;
      }

      const query = await savedQuerySvc.getQueryById(this.queryId);
      await this._hydrateFilters(query);
      this.query = query;
    },

    _hydrateFilters: async function(query) {
      for (const filter of query.filters) {
        if (filter.expr) {
          continue;
        }

        filter.fieldObj = await formCache.getField(query.cpId, query.cpGroupId, filter.field);
        if (filter.subQueryId > 0) {
          const subQuery = await savedQuerySvc.getQueryById(filter.subQueryId);
          filter.subQuery = {id: subQuery.id, title: subQuery.title};
        }
      }
    }
  }
}
</script>
