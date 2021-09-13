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
          <span v-show-if-allowed="'admin'">
            <span v-if="!ctx.selectedInstitutes || ctx.selectedInstitutes.length == 0">
              <os-button left-icon="plus" label="Create" @click="$goto('InstituteAddEdit', {instituteId: -1})" />

              <os-menu label="Import" :options="importOpts" />
            </span>
            <span v-else>
              <os-button left-icon="trash" label="Delete" @click="deleteInstitutes" />
            </span>
          </span>

          <os-button left-icon="download" label="Export" @click="exportInstitutes" />

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

      <os-confirm-delete ref="deleteDialog">
        <template #message>
          <span>Are you sure you want to delete the selected institutes?</span>
        </template>
      </os-confirm-delete>
    </os-page-body>
  </os-page>
</template>

<script>

import alertSvc     from '@/common/services/Alerts.js';
import exportSvc    from '@/common/services/ExportService.js';
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

      listSchema: { columns: [] },

      importOpts: [
        { icon: 'university', caption: 'Institutes',        onSelect: () => routerSvc.ngGoto('institutes-import') },
        { icon: 'table',      caption: 'View Past Imports', onSelect: () => routerSvc.ngGoto('institutes-import-jobs') }
      ]
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
      this.$goto('InstituteOverview', {instituteId: row.institute.id});
    },

    exportInstitutes: function() {
      let instituteIds = this.ctx.selectedInstitutes.map(institute => institute.id);
      exportSvc.exportRecords({objectType: 'institute', recordIds: instituteIds});
    },

    deleteInstitutes: function() {
      let instituteIds = this.ctx.selectedInstitutes.map(institute => institute.id);
      if (instituteIds.length <= 0) {
        return;
      }

      let self = this;
      this.$refs.deleteDialog.open().then(
        () => {
          instituteSvc.deleteInstitutes(instituteIds).then(
            (deleted) => {
              alertSvc.success(deleted.length + (deleted.length != 1 ? ' institutes ' : ' institute ') + ' deleted');
              self.$refs.listView.reload();
            }
          );
        }
      );
    }
  }
}
</script>
