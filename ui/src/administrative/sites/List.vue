<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'sites.list'">Sites</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.sites"
              :page-size="ctx.pageSize"
              :list-size="ctx.sitesCount"
              @updateListSize="getSitesCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <span v-show-if-allowed="'institute-admin'">
                <span v-if="!ctx.selectedSites || ctx.selectedSites.length == 0">
                  <os-button left-icon="plus" :label="$t('common.buttons.create')"
                    @click="$goto('SiteAddEdit', {siteId: -1}, {})" />

                  <os-menu :label="$t('common.buttons.import')" :options="importOpts" />

                  <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportSites" />
                </span>
                <span v-else>
                  <os-button left-icon="trash"    :label="$t('common.buttons.delete')" @click="deleteSites" />

                  <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportSites" />
                </span>
              </span>

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://help.openspecimen.org/sites" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :context="ctx.ui"
            :data="ctx.sites"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="true"
            :loading="ctx.loading"
            :selected="ctx.selectedSite"
            @filtersUpdated="loadSites"
            @selectedRows="onSitesSelection"
            @rowClicked="onSiteRowClick"
            ref="listView"
          />

          <os-confirm-delete ref="deleteDialog">
            <template #message>
              <span v-t="'sites.confirm_delete_selected'">Are you sure you want to delete the selected sites?</span>
            </template>
          </os-confirm-delete>
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.siteId > 0 && ctx.selectedSite">
      <router-view :siteId="ctx.selectedSite.site.id" :key="$route.params.siteId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import listSchema from '@/administrative/schemas/sites/list.js';

import alertSvc   from '@/common/services/Alerts.js';
import exportSvc  from '@/common/services/ExportService.js';
import routerSvc  from '@/common/services/Router.js';
import siteSvc    from '@/administrative/services/Site.js';

export default {
  props: ['siteId', 'filters'],

  data() {
    return {
      ctx: {
        ui: this.$ui,
        sites: [],
        sitesCount: -1,
        loading: true,
        query: this.filters,
        selectedSites: [],
        detailView: false,
        selectedSite: null
      },

      listSchema,

      importOpts: [
        {
          icon: 'hospital',
          caption: this.$t('sites.list'),
          onSelect: () => routerSvc.goto('SiteImportRecords')
        },
        {
          icon: 'table',
          caption: this.$t('bulk_imports.view_jobs'),
          onSelect: () => routerSvc.goto('SiteImportJobs')
        }
      ]
    };
  },

  watch: {
    'siteId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.ctx.sites.find(rowObject => rowObject.site.id == this.siteId);
        if (!selectedRow) {
          selectedRow = {site: {id: this.siteId}};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newValue == -2);
      }
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadSites: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;

      const sites = await this.reloadSites();
      if (this.siteId <= 0) {
        routerSvc.goto('SitesList', {siteId: -1}, {filters: uriEncoding});
      } else {
        let selectedRow = sites.find(rowObject => rowObject.site.id == this.siteId);
        if (!selectedRow) {
          selectedRow = {site: {id: this.siteId}};
        }

        this.showDetails(selectedRow);
      }
    },

    reloadSites: function() {
      this.ctx.loading = true;
      let opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      return siteSvc.getSites(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.sites = resp.map((site) => ({site: site}));
        return this.ctx.sites;
      });
    },

    getSitesCount: function() {
      this.ctx.sitesCount = -1;
      let opts = Object.assign({}, this.ctx.filterValues);
      siteSvc.getSitesCount(opts).then((resp) => this.ctx.sitesCount = resp.count);
    },

    onSitesSelection: function(selection) {
      this.ctx.selectedSites = (selection || []).map((row) => row.rowObject.site);
    },

    onSiteRowClick: function(rowObject) {
      routerSvc.goto('SitesListItemDetail.Overview', {siteId: rowObject.site.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedSite = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('SitesList', {siteId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    deleteSites: function() {
      let siteIds = this.ctx.selectedSites.map((site) => site.id);
      if (siteIds.length <= 0) {
        return;
      }

      let self = this;
      this.$refs.deleteDialog.open().then(
        () => {
          siteSvc.bulkUpdate({detail: {activityStatus: 'Disabled'}, ids: siteIds}).then(
            (saved) => {
              alertSvc.success({code: 'sites.deleted', args: {count: saved.length}});
              self.$refs.listView.reload();
            }
          );
        }
      );
    },

    exportSites: function() {
      let siteIds = this.ctx.selectedSites.map(site => site.id);
      exportSvc.exportRecords({objectType: 'site', recordIds: siteIds});
    }
  }
}
</script>
