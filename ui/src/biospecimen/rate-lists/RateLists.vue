<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <h3 v-t="'lab_services.rate_lists'">Rate Lists</h3>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.rateLists"
              :page-size="ctx.pageSize"
              :list-size="ctx.rateListsCount"
              @updateListSize="getRateListsCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="showCreateRateListDialog" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :context="ctx.ui"
            :data="ctx.rateLists"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="false"
            :loading="ctx.loading"
            :selected="ctx.selectedRateList"
            @filtersUpdated="loadRateLists"
            @rowClicked="onRateListRowClick"
            ref="listView"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.rateListId > 0 && ctx.selectedRateList">
      <router-view :rateListId="$route.params.rateListId" :key="$route.params.rateListId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import rateListSvc from '@/biospecimen/services/RateList.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['rateListId', 'filters'],

  data() {
    return {
      ctx: {
        ui: this.$ui,

        rateLists: [],

        rateListsCount: -1,

        loading: true,

        query: this.filters,

        detailView: false,

        selectedRateList: null
      },

      listSchema: rateListSvc.getListSchema()
    };
  },

  watch: {
    rateListId: function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.ctx.rateLists.find(({rateList}) => rateList.id == this.rateListId);
        if (!selectedRow) {
          //
          // TODO: load the rate list and add it to the list
          //
          selectedRow = {rateList: {id: this.rateListId}};
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

    loadRateLists: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const rateLists = await this._reloadRateLists();
      if (this.rateListId <= 0) {
        routerSvc.goto('RateLists', {rateListId: -1}, {filters: uriEncoding});
      } else {
        let selectedRow = rateLists.find(({rateList}) => rateList.id == this.rateListId);
        if (!selectedRow) {
          selectedRow = {rateList: {id: this.rateListId}};
        }

        this.showDetails(selectedRow);
      }
    },

    getRateListsCount: function() {
      this.ctx.rateListsCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues);
      rateListSvc.getRateListsCount(opts).then(({count}) => this.ctx.rateListsCount = count);
    },

    onRateListRowClick: function({rateList}) {
      routerSvc.goto('RateListsItemDetail.Overview', {rateListId: rateList.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedRateList = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('RateLists', {rateListId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    _reloadRateLists: function() {
      this.ctx.loading = true;
      const opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      return rateListSvc.getRateLists(opts).then(
        resp => {
          this.ctx.loading = false;
          this.ctx.rateLists = resp.map(rateList => ({rateList}));
          return this.ctx.rateLists;
        }
      );
    }
  }
}
</script>
