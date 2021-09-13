<template>
  <os-page>
    <os-page-head>
      <span>
        <h3>Institutes</h3>
      </span>

      <template #right>
        <os-list-size
          :list="ctx.institutes"
          :page-size="ctx.pageSize"
          :list-size="ctx.institutesCount"
          @updateListSize="getInstitutesCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-button-link left-icon="question-circle" label="Help"
            url="https://help.openspecimen.org/institute" new-tab="true" />
        </template>

        <template #right>
          <os-button left-icon="search" label="Search" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.institutes"
        :columns="listSchema.columns"
        :filters="listSchema.filters"
        :query="ctx.query"
        :allowSelection="true"
        :loading="ctx.loading"
        @filtersUpdated="loadInstitutes"
        @selectedRows="onInstituteSelection"
        @rowClicked="showDetails"
        ref="listView"
      />
    </os-page-body>
  </os-page>
</template>

<script>

import routerSvc    from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        institutes: [],
        institutesCount: -1,
        loading: true,
        query: this.filters,
        selectedInstitutes: []
      },

      listSchema: { columns: [] }
    };
  },

  created() {
    instituteSvc.getListViewSchema().then(listSchema => this.listSchema = listSchema);
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadInstitutes: function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      routerSvc.goto('InstitutesList', {}, {filters: uriEncoding});
      this.reloadInstitutes();
    },

    reloadInstitutes: function() {
      this.ctx.loading = true;
      let opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      instituteSvc.getInstitutes(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.institutes = resp.map(institute => ({institute: institute}));
      });
    },

    getInstitutesCount: function() {
      this.ctx.institutesCount = -1;
      let opts = Object.assign({}, this.ctx.filterValues);
      instituteSvc.getInstitutesCount(opts).then(resp => this.ctx.institutesCount = resp.count);
    },

    onInstituteSelection: function(selection) {
      this.ctx.selectedInstitutes = (selection || []).map((row) => row.rowObject.institute);
    },

    showDetails: function(row) {
      alert('Show details of ' + row.institute.name);
    },
  }
}
</script>
