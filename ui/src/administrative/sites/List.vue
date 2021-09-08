<template>
  <os-page>
    <os-page-head>
      <span>
        <h3>Sites</h3>
      </span>

      <template #right>
        <os-list-size
          :list="ctx.sites"
          :page-size="ctx.pageSize"
          :list-size="ctx.sitesCount"
          @updateListSize="getSitesCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <span v-show-if-allowed="'institute-admin'">
            <span v-if="!ctx.selectedSites || ctx.selectedSites.length == 0">
              <os-button left-icon="plus" label="Create" @click="$goto('SiteAddEdit', {siteId: -1}, {})" />

              <os-menu label="Import" :options="importOpts" />

              <os-button left-icon="download" label="Export" @click="exportSites" />
            </span>
            <span v-else>
              <os-button left-icon="trash"    label="Delete" @click="deleteSites" />

              <os-button left-icon="download" label="Export" @click="exportSites" />
            </span>
          </span>

          <os-button-link left-icon="question-circle" label="Help"
            url="https://help.openspecimen.org/sites" new-tab="true" />
        </template>

        <template #right>
          <os-button left-icon="search" label="Search" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.sites"
        :columns="listSchema.columns"
        :filters="listSchema.filters"
        :query="ctx.query"
        :allowSelection="true"
        :loading="ctx.loading"
        @filtersUpdated="loadSites"
        @selectedRows="onSitesSelection"
        @rowClicked="showSiteDetails"
        ref="listView"
      />

      <os-confirm-delete ref="deleteDialog">
        <template #message>
          <span>Are you sure you want to delete the selected sites?</span>
        </template>
      </os-confirm-delete>
    </os-page-body>
  </os-page>
</template>

<script>

import listSchema from './schemas/list.js';

import alertSvc   from '@/common/services/Alerts.js';
import exportSvc  from '@/common/services/ExportService.js';
import routerSvc  from '@/common/services/Router.js';
import siteSvc    from '@/administrative/services/Site.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        sites: [],
        sitesCount: -1,
        loading: true,
        query: this.filters,
        selectedSites: []
      },

      listSchema,

      importOpts: [
        { icon: 'hospital', caption: 'Sites',             onSelect: () => routerSvc.ngGoto('sites-import') },
        { icon: 'table',    caption: 'View Past Imports', onSelect: () => routerSvc.ngGoto('sites-import-jobs') }
      ]
    };
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadSites: function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;

      routerSvc.goto('SitesList', {}, {filters: uriEncoding});
      this.reloadSites();
    },

    reloadSites: function() {
      this.ctx.loading = true;
      let opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      siteSvc.getSites(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.sites = resp.map((site) => ({site: site}));
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

    showSiteDetails: function(row) {
      this.$goto('SiteOverview', {siteId: row.site.id}, {});
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
              alertSvc.success(saved.length + (saved.length != 1 ? ' sites ' : ' site ') + ' deleted');
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
