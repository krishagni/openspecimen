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

              <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportContainers"
                v-show-if-allowed="containerResources.importOpts" />
            </span>

            <span v-else>
              <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="createContainer"
                v-show-if-allowed="containerResources.createOpts" />

              <os-button left-icon="cubes" :label="$t('containers.types')" @click="viewContainerTypes" />

              <os-button left-icon="tasks" :label="$t('containers.tasks')" @click="viewContainerTasks" />

              <os-menu :label="$t('common.buttons.import')" :options="importOpts"
                v-show-if-allowed="containerResources.importOpts" />

              <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportContainers"
                v-show-if-allowed="containerResources.importOpts" />

              <os-menu :label="$t('common.buttons.more')" :options="moreOpts" />

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://help.openspecimen.org/containers" new-tab="true" />
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

import containerResources from './Resources.js';

export default {
  props: ['filters'],

  data() {
    const moreOpts = [];
    if (authSvc.isAllowed({resource: 'StorageContainer', operations: ['Update']})) {
      moreOpts.push({
        icon: 'archive',
        caption: this.$t('common.buttons.archive'),
        onSelect: () => routerSvc.goto('BulkContainerArchive')
      });
      moreOpts.push({
        icon: 'sign-in-alt',
        caption: this.$t('containers.check_in_button'),
        onSelect: () => routerSvc.goto('BulkContainerCheckin')
      });
      moreOpts.push({
        icon: 'sign-out-alt',
        caption: this.$t('containers.check_out_button'),
        onSelect: () => routerSvc.goto('BulkContainerCheckout')
      });
      moreOpts.push({
        icon: 'arrows-alt-h',
        caption: this.$t('containers.transfer'),
        onSelect: () => routerSvc.goto('BulkContainerTransfer')
      });
    }

    if (moreOpts.length > 0) {
      moreOpts.push({divider: true});
    }

    moreOpts.push({
      icon: 'arrows-alt-h',
      caption: this.$t('containers.transfer_report'),
      onSelect: () => this.showTransferReportDialog()
    });

    moreOpts.push({
      icon: 'file',
      caption: this.$t('containers.utilisation_report'),
      onSelect: () => this.showUtilisationReportDialog()
    });

    return {
      ctx: {
        containers: [],
        containersCount: -1,
        loading: true,
        query: this.filters,
        selectedContainers: [],
        trRptFs: containerSvc.getTransferReportFormSchema(),
      },

      listSchema,

      importOpts: [
        {
          icon: 'box-open',
          caption: this.$t('containers.list'),
          onSelect: () => routerSvc.ngGoto('containers-import')
        },
        {
          icon: 'table',
          caption: this.$t('bulk_imports.view_jobs'),
          onSelect: () => routerSvc.ngGoto('containers-import-jobs')
        }
      ],

      moreOpts: moreOpts,

      trRptCtx: {showDateRange: false, criteria: {}},

      containerResources
    };
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadContainers: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      await this.reloadContainers();
      routerSvc.goto('ContainersList', {}, {filters: uriEncoding});
    },

    reloadContainers: async function() {
      this.ctx.loading = true;
      const opts = {orderByStarred: true, includeStats: true, topLevelContainers: true, maxResults: this.ctx.pageSize};
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
      routerSvc.goto('ContainerDetail.Overview', {containerId: container.id}, {filters: this.filters});
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

    exportContainers: function() {
      const containerIds = this.ctx.selectedContainers.map(item => item.rowObject.container.id);
      exportSvc.exportRecords({objectType: 'storageContainer', recordIds: containerIds});
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
      this.$refs.utRptDialog.open()
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
    }
  }
}
</script>
