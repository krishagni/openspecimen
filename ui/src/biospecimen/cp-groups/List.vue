<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <h3>
            <span v-t="'cpgs.list'">Collection Protocol Groups</span>
          </h3>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.groups"
              :page-size="ctx.pageSize"
              :list-size="ctx.groupsCount"
              @updateListSize="getGroupsCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <os-button left-icon="plus" :label="$t('common.buttons.create')"
                v-show-if-allowed="{resource: 'CollectionProtocol', operations: ['Update']}" @click="createGroup" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.groups"
            :schema="listSchema"
            :loading="ctx.loading"
            :query="ctx.query"
            :selected="ctx.selectedGroup"
            @filtersUpdated="loadGroups"
            @rowClicked="onGroupRowClick"
            ref="listView"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.cpgId >= 0 && ctx.selectedGroup">
      <router-view :cpg-id="ctx.selectedGroup.cpg.id" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import listSchema from  '@/biospecimen/schemas/cp-groups/list.js';

import cpgSvc     from '@/biospecimen/services/CollectionProtocolGroup.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cpgId', 'filters'],

  data() {
    return {
      ctx: {
        groups: [],

        groupsCount: -1,

        loading: true,

        query: this.filters,

        detailView: false,

        selectedGroup: null
      },

      listSchema,
    };
  },

  created() { },

  watch: {
    cpgId: function(newCpgId, oldCpgId) {
      if (newCpgId == oldCpgId) {
        return;
      }

      if (newCpgId >= 0) {
        const selectedRow = this._findCpg(this.ctx.groups, newCpgId);
        this.showDetails(selectedRow);
      } else {
        this.showTable(newCpgId == -2);
      }
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadGroups: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const groups = await this._reloadGroups();
      if (this.cpgId < 0) {
        routerSvc.goto('CpgsList', {cpgId: -1}, {filters: uriEncoding});
      } else {
        const selectedRow = this._findCpg(groups, this.cpgId);
        this.showDetails(selectedRow);
      }
    },

    _findCpg: function(groups, cpgId) {
      const group = groups.find(({cpg}) => cpg.id == cpgId);
      if (!group) {
        return {cpg: {id: cpgId}};
      }

      return group;
    },

    _reloadGroups: async function() {
      this.ctx.loading = true;

      const opts   = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      const groups = await cpgSvc.getGroups(opts);

      this.ctx.loading = false;
      this.ctx.groups = groups.map(cpg => ({cpg}));
      return this.ctx.groups;
    },

    getGroupsCount: function() {
      this.ctx.groupsCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues || {});
      cpgSvc.getGroupsCount(opts).then(({count}) => this.ctx.groupsCount = count);
    },

    onGroupRowClick: function({cpg}) {
      routerSvc.goto('CpgDetail.Overview', {cpgId: cpg.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedGroup = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('CpgsList', {cpgId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    createGroup: function() {
      routerSvc.goto('CpgAddEdit', {cpgId: -1});
    },
  }
}
</script>
