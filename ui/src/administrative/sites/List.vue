<template>
  <Page>
    <PageHeader>
      <span>
        <h3>Sites</h3>
      </span>
    </PageHeader>
    <PageBody>
      <PageToolbar>
        <template #default> </template>
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
    ListView
  },

  data() {
    return {
      ctx: {
        sites: [],
        loading: true,
        query: this.filters
      },

      listSchema
    };
  },

  methods: {
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

    showSiteDetails: function() {
      alertSvc.underDev();
    }
  }
}
</script>
