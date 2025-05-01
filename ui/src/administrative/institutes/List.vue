<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3>{{$t("institutes.list")}}</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.institutes"
              :page-size="ctx.pageSize"
              :list-size="ctx.institutesCount"
              @updateListSize="getInstitutesCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <span v-show-if-allowed="'admin'">
                <span v-if="!ctx.selectedInstitutes || ctx.selectedInstitutes.length == 0">
                  <os-button left-icon="plus" :label="$t('common.buttons.create')"
                    @click="$goto('InstituteAddEdit', {instituteId: -1})" />

                  <os-menu :label="$t('common.buttons.import')" :options="importOpts" />
                </span>
                <span v-else>
                  <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteInstitutes" />
                </span>
              </span>

              <os-button left-icon="download" :label="$t('common.buttons.export')" @click="exportInstitutes" />

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://help.openspecimen.org/institute" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.institutes"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="true"
            :loading="ctx.loading"
            :selected="ctx.selectedInstitute"
            @filtersUpdated="loadInstitutes"
            @selectedRows="onInstituteSelection"
            @rowClicked="onInstituteRowClick"
            ref="listView"
          />

          <os-confirm-delete ref="deleteDialog">
            <template #message>
              <span v-t="'institutes.confirm_delete_selected'">
                Are you sure you want to delete the selected institutes?
              </span>
            </template>
          </os-confirm-delete>
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.instituteId > 0 && ctx.selectedInstitute">
      <router-view :instituteId="ctx.selectedInstitute.institute.id" :key="$route.params.instituteId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import alertSvc     from '@/common/services/Alerts.js';
import exportSvc    from '@/common/services/ExportService.js';
import routerSvc    from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  props: ['filters', 'instituteId'],

  data() {
    return {
      ctx: {
        institutes: [],
        institutesCount: -1,
        loading: true,
        query: this.filters,
        selectedInstitutes: [],
        selectedInstitute: null,
        detailView: false
      },

      listSchema: { columns: [] },

      importOpts: [
        {
          icon: 'university',
          caption: this.$t('institutes.list'),
          onSelect: () => routerSvc.goto('InstituteImportRecords')
        },
        {
          icon: 'table',
          caption: this.$t('bulk_imports.view_jobs'),
          onSelect: () => routerSvc.goto('InstituteImportJobs')
        }
      ]
    };
  },

  created() {
    instituteSvc.getListViewSchema().then(listSchema => this.listSchema = listSchema);
  },

  watch: {
    'instituteId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.ctx.institutes.find(rowObject => rowObject.institute.id == this.instituteId);
        if (!selectedRow) {
          selectedRow = {institute: {id: this.instituteId}};
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

    loadInstitutes: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const institutes = await this.reloadInstitutes();
      if (this.instituteId <= 0) {
        routerSvc.goto('InstitutesList', {instituteId: -1}, {filters: uriEncoding});
      } else {
        let selectedRow = institutes.find(rowObject => rowObject.institute.id == this.instituteId);
        if (!selectedRow) {
          selectedRow = {institute: {id: this.instituteId}};
        }

        this.showDetails(selectedRow);
      }
    },

    reloadInstitutes: function() {
      this.ctx.loading = true;
      let opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      return instituteSvc.getInstitutes(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.institutes = resp.map(institute => ({institute: institute}));
        return this.ctx.institutes;
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

    onInstituteRowClick: function(rowObject) {
      routerSvc.goto(
        'InstitutesListItemDetail.Overview',
        {instituteId: rowObject.institute.id},
        {filters: this.filters}
      );
    },

    showDetails: function(rowObject) {
      this.ctx.selectedInstitute = rowObject;
      if (!this.ctx.detailView) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('InstitutesList', {instituteId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
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
              alertSvc.success(this.$t('institutes.deleted', deleted.length, {count: deleted.length}));
              self.$refs.listView.reload();
            }
          );
        }
      );
    }
  }
}
</script>
