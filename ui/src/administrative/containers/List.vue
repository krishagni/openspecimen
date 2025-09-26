<template>
  <os-page>
    <os-page-head>
      <h3>
        <span v-t="'containers.list'">Containers</span>
      </h3>

      <template #right>
        <os-list-size
          :list="ctx.containers"
          :page-size="ctx.pageSize"
          :list-size="ctx.containersCount"
          @updateListSize="getContainersCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <span>
            <span v-if="ctx.selectedContainers && ctx.selectedContainers.length > 0">
              <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteContainers"
                v-show-if-allowed="containerResources.deleteOpts" />

              <os-menu :label="$t('common.buttons.export')" :options="exportOpts" />
            </span>

            <span v-else>
              <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="createContainer"
                v-show-if-allowed="containerResources.createOpts" />

              <os-button left-icon="cubes" :label="$t('containers.types')" @click="viewContainerTypes" />

              <os-button left-icon="tasks" :label="$t('containers.tasks')" @click="viewContainerTasks" />

              <os-menu :label="$t('common.buttons.import')" :options="importOpts"
                v-show-if-allowed="containerResources.importOpts" />

              <os-menu :label="$t('common.buttons.actions')" :options="actionOpts"
                v-if="actionOpts && actionOpts.length > 0" />

              <os-menu :label="$t('common.buttons.export')" :options="exportOpts" />

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://openspecimen.atlassian.net/wiki/x/a4CB" new-tab="true" />
            </span>
          </span>
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.containers"
        :schema="listSchema"
        :query="ctx.query"
        :allowSelection="true"
        :loading="ctx.loading"
        @filtersUpdated="loadContainers"
        @rowClicked="onContainerRowClick"
        @selectedRows="onContainersSelection"
        @rowStarToggled="onToggleStar"
        ref="listView"
      />

      <os-confirm-delete ref="deleteDialog">
        <template #message>
          <span v-t="'containers.confirm_delete_selected'">
            Are you sure you want to delete the selected containers?
          </span>
        </template>
      </os-confirm-delete>

      <os-dialog ref="trRptDialog">
        <template #header>
          <span v-t="'containers.transfer_report'"> Transfer Report </span>
        </template>
        <template #content>
          <os-form ref="transferRpt" :schema="ctx.trRptFs.layout" :data="trRptCtx" />
        </template>
        <template #footer>
          <os-button text    :label="$t('common.buttons.cancel')"   @click="closeTransferReportDialog" />
          <os-button primary :label="$t('common.buttons.generate')" @click="generateTransferReport" />
        </template>
      </os-dialog>

      <os-dialog ref="utRptDialog">
        <template #header>
          <span v-t="'containers.utilisation_report'"> Utilisation Report </span>
        </template>
        <template #content>
          <os-form ref="utilisationRpt" :schema="ctx.trRptFs.layout" :data="trRptCtx" />
        </template>
        <template #footer>
          <os-button text    :label="$t('common.buttons.cancel')"   @click="closeUtilisationReportDialog" />
          <os-button primary :label="$t('common.buttons.generate')" @click="generateUtilisationReport" />
        </template>
      </os-dialog>

      <os-dialog ref="exportContainersDialog">
        <template #header>
          <span v-t="'containers.export_containers'"> Export Containers </span>
        </template>
        <template #content>
          <os-form ref="exportCritForm" :schema="ctx.trRptFs.layout" :data="trRptCtx" />
        </template>
        <template #footer>
          <os-button text    :label="$t('common.buttons.cancel')"   @click="closeExportContainersDialog" />
          <os-button primary :label="$t('common.buttons.export')" @click="exportContainers" />
        </template>
      </os-dialog>
    </os-page-body>
  </os-page>
</template>

<script>

import listSchema   from '@/administrative/schemas/containers/list.js';

import containerSvc from '@/administrative/services/Container.js';
import alertsSvc    from '@/common/services/Alerts.js';
import authSvc      from '@/common/services/Authorization.js';
import exportSvc    from '@/common/services/ExportService.js';
import routerSvc    from '@/common/services/Router.js';
import util         from '@/common/services/Util.js';
import wfSvc        from '@/common/services/Workflow.js';

import containerResources from './Resources.js';

export default {
  props: ['filters', 'show-stats'],

  data() {
    const exportOpts = [];
    exportOpts.push({
      icon: 'arrows-alt-h',
      caption: this.$t('containers.transfer_report'),
      onSelect: () => this.showTransferReportDialog()
    });

    exportOpts.push({
      icon: 'file',
      caption: this.$t('containers.utilisation_report'),
      onSelect: () => this.showUtilisationReportDialog()
    });

    if (authSvc.isAllowed(containerResources.importOpts)) {
      exportOpts.push({divider: true});

      exportOpts.push({
        icon: 'download',
        caption: this.$t('containers.list'),
        onSelect: () => this.showExportContainersDialog()
      });
    }

    return {
      ctx: {
        containers: [],
        containersCount: -1,
        loading: true,
        query: this.filters,
        selectedContainers: [],
        trRptFs: containerSvc.getTransferReportFormSchema(),
        showStats: false
      },

      importOpts: [
        {
          icon: 'box-open',
          caption: this.$t('containers.list'),
          onSelect: () => routerSvc.goto('ContainerImportRecords')
        },
        {
          icon: 'table',
          caption: this.$t('bulk_imports.view_jobs'),
          onSelect: () => routerSvc.goto('ContainerImportJobs')
        }
      ],

      exportOpts,

      trRptCtx: {showDateRange: false, criteria: {}},

      containerResources
    };
  },

  computed: {
    listSchema: function() {
      const result = {...listSchema};
      if (!util.isTrue(this.showStats)) {
        result.columns = result.columns.filter(({name}) => name != 'container.storedSpecimens' && name != 'container.utilisation');
      }

      return result;
    },

    actionOpts: function() {
      const actionOpts = [];
      if (authSvc.isAllowed(containerResources.updateOpts)) {
        actionOpts.push({
          icon: 'archive',
          caption: this.$t('common.buttons.archive'),
          onSelect: () => routerSvc.goto('BulkContainerArchive')
        });
        actionOpts.push({
          icon: 'sign-in-alt',
          caption: this.$t('containers.check_in_button'),
          onSelect: () => routerSvc.goto('BulkContainerCheckin')
        });
        actionOpts.push({
          icon: 'sign-out-alt',
          caption: this.$t('containers.check_out_button'),
          onSelect: () => routerSvc.goto('BulkContainerCheckout')
        });
        actionOpts.push({
          icon: 'arrows-alt-h',
          caption: this.$t('containers.transfer'),
          onSelect: () => routerSvc.goto('BulkContainerTransfer')
        });
        actionOpts.push({
          icon: 'box',
          caption: this.$t('containers.find_places.title'),
          onSelect: () => routerSvc.goto('ContainerFindPlaces')
        });
        actionOpts.push({
          icon: 'check',
          caption: this.$t('containers.unblock'),
          onSelect: () => routerSvc.goto('UnblockLocations')
        });

        if (this.ctx.scanners && this.ctx.scanners.length > 0) {
          actionOpts.push({
            icon: 'qrcode',
            caption: this.$t('containers.scan_boxes'),
            onSelect: () => routerSvc.goto('ScanBoxes')
          });
        }
      }

      if (actionOpts.length > 0) {
        actionOpts.push({divider: true});
      }

      const showStats = util.isTrue(this.showStats);
      actionOpts.push({
        icon: 'poll',
        caption: this.$t(showStats ? 'containers.hide_stats' : 'containers.view_stats'),
        onSelect: () => this.toggleViewStats()
      });

      return actionOpts;
    }
  },

  watch: {
    showStats: function(newVal, oldVal) {
      const oldShowStats = util.isTrue(oldVal);
      const newShowStats = util.isTrue(newVal);
      if (newShowStats == oldShowStats) {
        return;
      }

      this.reloadContainers();
    }
  },

  created() {
    wfSvc.getSysWorkflow('box-scanners').then(
      wfData => {
        const {scanners} = wfData || {};
        this.ctx.scanners = scanners;
      }
    );
  },

  methods: {
    toggleViewStats: function() {
      const {name, params, query} = routerSvc.getCurrentRoute();
      const otherParams = util.isTrue(this.showStats) ? {showStats: false} : {showStats: true};
      routerSvc.replace(name, params, { ...query, ...otherParams });
    },

    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadContainers: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      await this.reloadContainers();
      routerSvc.goto('ContainersList', {}, {filters: uriEncoding, showStats: this.showStats});
    },

    reloadContainers: async function() {
      this.ctx.loading = true;

      const includeStats = util.isTrue(this.showStats);
      const opts = {orderByStarred: true, includeStats, topLevelContainers: true, maxResults: this.ctx.pageSize};
      const containers = await containerSvc.getContainers(Object.assign(opts, this.ctx.filterValues || {}));
      this.ctx.containers = containers.map(container => ({ container }));
      this.ctx.loading = false;
      this.ctx.selectedContainers = [];
      return this.ctx.containers;
    },

    getContainersCount: async function() {
      const opts = Object.assign({topLevelContainers: true}, this.ctx.filterValues);
      const resp = await containerSvc.getContainersCount(opts);
      this.ctx.containersCount = resp.count;
    },

    onContainerRowClick: function({container}) {
      routerSvc.goto('ContainerDetail.Overview', {containerId: container.id}, {filters: this.filters, showStats: this.showStats});
    },

    onContainersSelection: function(selection) {
      this.ctx.selectedContainers = selection;
    },

    onToggleStar: async function({container}) {
      let resp;
      if (container.starred) {
        resp = await containerSvc.unstar(container.id);
      } else {
        resp = await containerSvc.star(container.id);
      }

      if (resp.status) {
        container.starred = !container.starred;
      }
    },

    showTransferForm: function() {
      this.$goto('BulkContainerTransfer');
    },

    deleteContainers: async function() {
      const containerIds = this.ctx.selectedContainers.map(item => item.rowObject.container.id);
      await this.$refs.deleteDialog.open();
      await containerSvc.bulkDelete(containerIds, true);
      alertsSvc.success({code: 'containers.bulk_deleted'});
      this.$refs.listView.reload();
    },

    createContainer: function() {
      routerSvc.goto('ContainerAddEdit', {containerId: -1});
    },

    viewContainerTypes: function() {
      routerSvc.goto('ContainerTypesList', {typeId: -1});
    },

    viewContainerTasks: function() {
      routerSvc.goto('ContainerTasksList');
    },

    showTransferReportDialog: function() {
      this.trRptCtx = {showDateRange: true, criteria: {}};
      if (this.ctx.selectedContainers.length > 0) {
        this.trRptCtx.criteria.name = this.ctx.selectedContainers.map(item => item.rowObject.container.name);
        this.generateTransferReport();
        return;
      }

      this.$refs.trRptDialog.open()
    },

    closeTransferReportDialog: function() {
      this.$refs.trRptDialog.close();
    },

    generateTransferReport: function() {
      alertsSvc.info({code: 'containers.generating_transfer_report'});
      containerSvc.exportTransferEvents(this.trRptCtx.criteria).then(
        (report) => {
          if (report.completed) {
            alertsSvc.info({code: 'containers.downloading_transfer_report'});
            containerSvc.downloadReport(report.name);
          } else {
            alertsSvc.info({code: 'containers.transfer_report_by_email'});
          }

          this.closeTransferReportDialog();
        }
      );
    },

    showUtilisationReportDialog: function() {
      this.trRptCtx = {showDateRange: false, criteria: {}};
      if (this.ctx.selectedContainers.length > 0) {
        this.trRptCtx.criteria.name = this.ctx.selectedContainers.map(item => item.rowObject.container.name);
        this.generateUtilisationReport();
        return;
      }

      this.$refs.utRptDialog.open();
    },

    closeUtilisationReportDialog: function() {
      this.$refs.utRptDialog.close();
    },

    generateUtilisationReport: function() {
      alertsSvc.info({code: 'containers.generating_utilisation_report'});
      containerSvc.exportUtilisation(null, this.trRptCtx.criteria).then(
        (report) => {
          if (report.fileId) {
            alertsSvc.info({code: 'containers.downloading_utilisation_report'});
            containerSvc.downloadReport(report.fileId);
          } else {
            alertsSvc.info({code: 'containers.utilisation_report_by_email'});
          }

          this.closeUtilisationReportDialog();
        }
      );
    },

    showExportContainersDialog: function() {
      if (this.ctx.selectedContainers.length > 0) {
        this.exportContainers();
        return;
      }

      this.trRptCtx = {showDateRange: false, criteria: {}};
      this.$refs.exportContainersDialog.open();
    },

    closeExportContainersDialog: function() {
      this.$refs.exportContainersDialog.close();
    },

    exportContainers: function() {
      const containerIds = this.ctx.selectedContainers.map(item => item.rowObject.container.id);
      const params = {};
      if (containerIds.length == 0) {
        const crit = this.trRptCtx.criteria;
        if (crit.name && crit.name.length > 0) {
          params.names = crit.name.join('^');
        }

        if (crit.cp && crit.cp.length > 0) {
          params.cps = crit.cp.join('^');
        }

        if (crit.type && crit.type.length > 0) {
          params.types = crit.type.join('^');
        }
      }

      exportSvc.exportRecords({objectType: 'storageContainer', recordIds: containerIds, params});
      this.closeExportContainersDialog();
    }
  }
}
</script>
