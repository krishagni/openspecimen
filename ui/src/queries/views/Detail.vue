<template>
  <router-view :query="query" :key="query.id" v-if="query" @query-saved="reloadQuery" />
</template>

<script>

import formCache from '@/queries/services/FormsCache.js';
import queriesCache from '@/queries/services/QueriesCache.js';
import routerSvc from '@/common/services/Router.js';
import savedQuerySvc from '@/queries/services/SavedQuery.js';

export default {
  props: ['queryId'],

  beforeRouteEnter(to, from, next) {
    const {osQuery} = window;
    const {route} = osQuery || {};
    if (!from || !route || route.name != from.name) {
      delete window.osQuery;
    }

    next();
  },

  data() {
    return {
      query: null
    }
  },

  created() {
    const {osQuery} = window;
    const {query} = osQuery;
    if (query) {
      this.query = JSON.parse(query);
      delete window.osQuery;
    } else {
      this._loadQuery();
    }
  },

  mounted() {
    queriesCache.init();
  },

  unmounted() {
    queriesCache.destroy();
  },

  watch: {
    queryId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        queriesCache.destroy();
        queriesCache.init();
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
        this.query = {
          queryExpression: [],
          filters: [],
          selectList: [],
          wideRowMode: 'DEEP',
          outputIsoDateTime: true,
          caseSensitive: true
        };

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
          filter.subQuery = await queriesCache.getQuery(filter.subQueryId);
          await this._hydrateFilters(filter.subQuery);
        }
      }
    }
  }
}
</script>
