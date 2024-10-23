<template>
  <os-page>
    <os-page-head>
      <h3>
        <span v-t="'queries.list'">Queries</span>
      </h3>

      <template #right>
        <os-list-size
          :list="ctx.queries"
          :page-size="ctx.pageSize"
          :list-size="ctx.queriesCount"
          @updateListSize="getQueriesCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <span>actions</span>
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.queries"
        :schema="listSchema"
        :query="ctx.query"
        :allowSelection="true"
        :loading="ctx.loading"
        :showRowActions="true"
        @filtersUpdated="loadQueries"
        @rowClicked="onQueryRowClick"
        @selectedRows="onQueriesSelection"
        @rowStarToggled="onToggleStar"
        ref="listView">

        <template #rowActions="{rowObject}">
          <os-menu icon="ellipsis-v" :options="queryOptions(rowObject.query)" />
        </template>
      </os-list-view>

      <os-confirm ref="deleteQueryDialog">
        <template #title>
          <span v-t="'queries.delete_query'">Delete Query</span>
        </template>
        <template #message>
          <span v-t="{path: 'queries.confirm_delete_query', args: ctx.query}"></span>
        </template>
      </os-confirm>
    </os-page-body>
  </os-page>
</template>

<script>

import listSchema   from '@/queries/schemas/list.js';

import alertsSvc     from '@/common/services/Alerts.js';
import authSvc       from '@/common/services/Authorization.js';
import routerSvc     from '@/common/services/Router.js';
import savedQuerySvc from '@/queries/services/SavedQuery.js';

import queryResources from './Resources.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        queries: [],

        queriesCount: -1,

        loading: true,

        query: this.filters,

        selectedQueries: []
      },

      listSchema
    };
  },

  created() {
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadQueries: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      await this.reloadQueries();
      routerSvc.goto('QueriesList', {}, {filters: uriEncoding});
    },

    reloadQueries: async function() {
      this.ctx.loading = true;
      const opts = {orderByStarred: true, returnList: true, maxResults: this.ctx.pageSize};
      const queries = await savedQuerySvc.getQueries(Object.assign(opts, this.ctx.filterValues || {}));

      this.ctx.queries = queries.map(query => ({ query }));
      this.ctx.loading = false;
      this.ctx.selectedQueries = [];
      return this.ctx.queries;
    },

    getQueriesCount: async function() {
      const opts = Object.assign({}, this.ctx.filterValues);
      const resp = await savedQuerySvc.getQueriesCount(opts);
      this.ctx.queriesCount = resp.count;
    },

    queryOptions: function(query) {
      const options = [];
      if (authSvc.isAllowed(queryResources.updateOpts)) {
        options.push({
          icon: 'edit',
          caption: this.$t('common.buttons.edit'),
          onSelect: () => this._editQuery(query)
        });
      }

      options.push({
        icon: 'download',
        caption: this.$t('common.buttons.download'),
        onSelect: () => this._downloadQuery(query)
      });
        
      if (authSvc.isAllowed({resource: 'ScheduledJob', operations: ['Create']})) {
        options.push({
          icon: 'calendar',
          caption: this.$t('queries.schedule'),
          onSelect: () => this._scheduleQuery(query)
        });
      }

      if (authSvc.isAllowed(queryResources.deleteOpts)) {
        options.push({
          icon: 'trash',
          caption: this.$t('common.buttons.delete'),
          onSelect: () => this._deleteQuery(query)
        });
      }

      return options;
    },

    onQueryRowClick: function({query}) {
      routerSvc.goto('QueryDetail.Results', {queryId: query.id});
    },

    onQueriesSelection: function(selection) {
      this.ctx.selectedQueries = selection;
    },

    onToggleStar: async function({query}) {
      let resp;
      if (query.starred) {
        resp = await savedQuerySvc.unstar(query.id);
      } else {
        resp = await savedQuerySvc.star(query.id);
      }

      if (resp.status) {
        query.starred = !query.starred;
      }
    },

    _editQuery: function(query) {
      routerSvc.goto('QueryDetail.AddEdit', {queryId: query.id});
    },

    _downloadQuery: function(query) {
      savedQuerySvc.downloadQuery(query.id);
    },

    _scheduleQuery: function(query) {
      routerSvc.goto('JobAddEdit', {jobId: -1}, {queryId: query.id});
    },

    _deleteQuery: function(query) {
      this.ctx.query = query;
      this.$refs.deleteQueryDialog.open().then(
        (resp) => {
          if (resp != 'proceed') {
            return;
          }

          savedQuerySvc.deleteQuery(query.id).then(
            () => {
              alertsSvc.success({code: 'queries.deleted', args: query});
              this.reloadQueries();
            }
          );
        }
      );
    }
  }
}
</script>
