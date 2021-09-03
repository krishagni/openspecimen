<template>
  <Page>
    <PageHeader>
      <span>
        <h3>Sites</h3>
      </span>

      <template #right>
        <ListSize
          :list="ctx.sites"
          :page-size="ctx.pageSize"
          :list-size="ctx.sitesCount"
          @updateListSize="getSitesCount"
        />
      </template>
    </PageHeader>
    <PageBody>
      <PageToolbar>
        <template #default>
          <span v-if="!ctx.selectedSites || ctx.selectedSites.length == 0">
            <Button left-icon="plus" label="Create" @click="createSite" />

            <Menu label="Import" :options="importOpts" />

            <Button left-icon="download" label="Export" @click="exportSites" />

            <ButtonLink left-icon="question-circle" label="Help"
              url="https://help.openspecimen.org/sites" new-tab="true" />
          </span>
          <span v-else>
            <Button left-icon="trash"    label="Delete" @click="deleteSites" />

            <Button left-icon="download" label="Export" @click="exportSites" />
          </span>
        </template>

        <template #right>
          <Button left-icon="search" label="Search" @click="openSearch" />
        </template>
      </PageToolbar>

      <ListView
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

      <ConfirmDelete ref="deleteDialog">
        <template #message>
          <span>Are you sure you want to delete the selected sites?</span>
        </template>
      </ConfirmDelete>
    </PageBody>
  </Page>
</template>

<script>

import Page        from '@/common/components/Page.vue';
import PageHeader  from '@/common/components/PageHeader.vue';
import PageBody    from '@/common/components/PageBody.vue';
import PageToolbar from '@/common/components/PageToolbar.vue';
import ListView    from '@/common/components/ListView.vue';
import ListSize    from '@/common/components/ListSize.vue';
import Button      from '@/common/components/Button.vue';
import ButtonLink  from '@/common/components/ButtonLink.vue';
import Menu        from '@/common/components/Menu.vue';
import ConfirmDelete from '@/common/components/ConfirmDelete.vue';

import listSchema from './schemas/list.js';

import alertSvc   from '@/common/services/Alerts.js';
import exportSvc  from '@/common/services/ExportService.js';
import routerSvc  from '@/common/services/Router.js';
import siteSvc    from '@/administrative/services/Site.js';

export default {
  props: ['filters'],

  components: {
    Page,
    PageHeader,
    PageBody,
    PageToolbar,
    ListView,
    ListSize,
    Button,
    ButtonLink,
    Menu,
    ConfirmDelete
  },

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

    showSiteDetails: function() {
      alertSvc.underDev();
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
            function(saved) {
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
    },

    createSite: function() {
      alertSvc.underDev();
    }
  }
}
</script>
