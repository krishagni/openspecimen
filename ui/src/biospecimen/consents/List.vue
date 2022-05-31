<template>
  <os-page>
    <os-page-head>
      <span>
        <h3>Consent Statements</h3>
      </span>

      <template #right>
        <os-list-size
          :list="ctx.statements"
          :page-size="ctx.pageSize"
          :list-size="ctx.statementsCount"
          @updateListSize="getStatementsCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-button left-icon="plus" label="Create" @click="$goto('ConsentStatementAddEdit', {statementId: -1})" />

          <os-plugin-views page="consent-statements" view="toolbar" />
        </template>

        <template #right>
          <os-button left-icon="search" label="Search" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.statements"
        :schema="listSchema"
        :query="ctx.query"
        :loading="ctx.loading"
        @filtersUpdated="loadStatements"
        @rowClicked="onStatementRowClick"
        ref="listView"
      />
    </os-page-body>
  </os-page>
</template>

<script>

import listSchema from  '@/biospecimen/schemas/consents/list.js';

import consentStmtSvc from '@/biospecimen/services/ConsentStatement.js';
import routerSvc      from '@/common/services/Router.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        statements: [],

        statementsCount: -1,

        loading: true,

        query: this.filters,
      },

      listSchema,
    };
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadStatements: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      await this.reloadStatements();
      routerSvc.goto('ConsentStatementsList', {}, {filters: uriEncoding});
    },

    reloadStatements: async function() {
      this.ctx.loading = true;

      const opts       = Object.assign({maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      const statements = await consentStmtSvc.getStatements(opts);

      this.ctx.loading = false;
      this.ctx.statements = statements.map(statement => ({ statement }));
      return this.ctx.statements;
    },

    getStatementsCount: function() {
      this.ctx.statementsCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues);
      consentStmtSvc.getStatementsCount(opts).then(({count}) => this.ctx.statementsCount = count);
    },

    onStatementRowClick: function({statement}) {
      routerSvc.goto('ConsentStatementAddEdit', {statementId: statement.id});
    },
  }
}
</script>
