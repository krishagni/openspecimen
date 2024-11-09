<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <h3>
        <span v-t="'queries.audit_logs'">Query Audit Logs</span>
      </h3>

      <template #right>
        <os-list-size
          :list="ctx.auditLogs"
          :page-size="ctx.pageSize"
          :list-size="ctx.auditLogsCount"
          @updateListSize="getLogsCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-button left-icon="download" :label="$t('common.buttons.export')" @click="showExportLogsDialog" />
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.auditLogs"
        :schema="listSchema"
        :query="ctx.query"
        :loading="ctx.loading"
        @filtersUpdated="loadLogs"
        @rowClicked="showQuerySqlDialog"
        ref="listView"
      />
    </os-page-body>
  </os-page>

  <os-dialog ref="exportLogsDialog">
    <template #header>
      <span v-t="'queries.export_audit_report'">Export Audit Report</span>
    </template>
    <template #content>
      <os-form ref="exportLogsForm" :schema="exportLogsFs" :data="auditReportCtx" />
    </template>
    <template #footer>
      <os-button text :label="$t('common.buttons.cancel')" @click="hideExportLogsDialog" />
      <os-button primary :label="$t('common.buttons.export')" @click="exportLogs" />
    </template>
  </os-dialog>

  <os-dialog ref="querySqlDialog">
    <template #header>
      <span>{{ctx.auditLog.queryTitle}}</span>
    </template>
    <template #content>
      <pre class="sql-code">{{ctx.auditLogSql}}</pre>
    </template>
    <template #footer>
      <os-button primary :label="$t('common.buttons.copy')" @click="copySqlToClipboard" />
      <os-button primary :label="$t('common.buttons.done')" @click="hideQuerySqlDialog" />
    </template>
  </os-dialog>
</template>

<script>
import exportLogsSchema from '@/queries/schemas/export-logs.js';
import listSchema   from '@/queries/schemas/audit-logs-list.js';

import alertsSvc    from '@/common/services/Alerts.js';
import auditLogsSvc from '@/queries/services/QueryAudit.js';
import i18n         from '@/common/services/I18n.js';
import routerSvc    from '@/common/services/Router.js';
import util         from '@/common/services/Util.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        bcrumb: [
          {url: routerSvc.getUrl('QueriesList', {}), label: i18n.msg('queries.list')}
        ],

        auditLogs: [],

        auditLogsCounts: -1,

        loading: true,

        query: this.filters
      },

      auditReportCtx: {},

      listSchema,

      exportLogsFs: exportLogsSchema.layout
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadLogs: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      await this.reloadLogs();
      routerSvc.goto('QueryAuditLogs', {}, {filters: uriEncoding});
    },

    reloadLogs: async function() {
      this.ctx.loading = true;
      const opts = {maxResults: this.ctx.pageSize};
      const logs = await auditLogsSvc.getLogs(Object.assign(opts, this.ctx.filterValues || {}));

      this.ctx.auditLogs = logs.map(auditLog => ({ auditLog }));
      this.ctx.loading = false;
      return this.ctx.logs;
    },

    getLogsCount: async function() {
      const {count} = await auditLogsSvc.getLogsCount(this.ctx.filterValues);
      this.ctx.auditLogsCount = count;
    },

    showQuerySqlDialog: function({auditLog}) {
      this.ctx.auditLog = auditLog;
      auditLogsSvc.getLog(auditLog.id).then(
        ({sql}) => {
          this.ctx.auditLogSql = sql;
          this.$refs.querySqlDialog.open();
        }
      );
    },

    hideQuerySqlDialog: function() {
      this.$refs.querySqlDialog.close();
    },

    copySqlToClipboard: function() {
      util.copyToClipboard(this.ctx.auditLogSql).then(
        () => alertsSvc.success({code: 'common.copied_to_clipboard'})
      );
    },

    showExportLogsDialog: function() {
      this.auditReportCtx = {criteria: {}};
      this.$refs.exportLogsDialog.open();
    },

    hideExportLogsDialog: function() {
      this.$refs.exportLogsDialog.close();
    },

    exportLogs: function() {
      const {startDate, endDate, user} = this.auditReportCtx.criteria;
      const criteria = {startDate, endDate, userIds: (user && [user.id]) || null};
      auditLogsSvc.exportLogs(criteria).then(
        ({fileId}) => {
          if (fileId) {
            alertsSvc.info({code: 'queries.downloading_audit_log_report'});
            auditLogsSvc.downloadReport(fileId);
          } else {
            alertsSvc.info({code: 'queries.audit_log_report_emailed'});
          }

          this.hideExportLogsDialog();
        }
      );
    }
  }
}
</script>

<style scoped>
.sql-code {
  white-space: pre-wrap;
  word-break: break-all;
  word-wrap: break-word;
  background-color: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 0.25rem;
  padding: 0.5rem;
  margin: 0 0 0.625rem;
}
</style>
