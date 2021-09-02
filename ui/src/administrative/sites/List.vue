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
        <template #default> </template>

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
        @rowClicked="showSiteDetails"
        ref="listView"
      />
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

import listSchema from './schemas/list.js';

import alertSvc   from '@/common/services/Alerts.js';
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
    Button
  },

  data() {
    return {
      ctx: {
        sites: [],
        sitesCount: -1,
        loading: true,
        query: this.filters
      },

      listSchema
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

    showSiteDetails: function() {
      alertSvc.underDev();
    }
  }
}
</script>
