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
              <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="createRateList" />
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
      <router-view :rate-list-id="$route.params.rateListId" :key="$route.params.rateListId"
        @rate-list-saved="updateRateList($event)"
        @rate-list-services-added="addServicesCount($event)"
        @rate-list-cps-added="addCpsCount($event)"
        @rate-list-cps-removed="deductCpsCount($event)" />
    </os-screen-panel>

    <AddEditRateList ref="addRateListDialog" />
  </os-screen>
</template>

<script>

import rateListSvc from '@/biospecimen/services/RateList.js';
import routerSvc  from '@/common/services/Router.js';

import AddEditRateList from './AddEditRateList.vue';

export default {
  props: ['rateListId', 'filters'],

  components: {
    AddEditRateList
  },

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

    createRateList: function() {
      this.$refs.addRateListDialog.open({rateList: {}}).then(
        savedRateList => {
          const newRow = {rateList: savedRateList};
          this.ctx.rateLists.unshift(newRow);
          this.onRateListRowClick(newRow);
        }
      );
    },

    updateRateList: function(savedRateList) {
      const row = this.ctx.rateLists.find(({rateList}) => rateList.id == savedRateList.id);
      if (row) {
        Object.assign(row.rateList, savedRateList);
      }
    },

    addServicesCount: function({rateList, count}) {
      this._updateServicesCount(rateList, count);
    },

    addCpsCount: function({rateList, count}) {
      this._updateCpsCount(rateList, count);
    },

    deductCpsCount: function({rateList, count}) {
      this._updateCpsCount(rateList, -count);
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
    },

    _updateServicesCount: function(rateList, count) {
      this._updateCount(rateList, 'servicesCount', count);
    },

    _updateCpsCount: function(rateList, count) {
      this._updateCount(rateList, 'cpsCount', count);
    },

    _updateCount: function(rateList, stat, count) {
      const row = this.ctx.rateLists.find(rowObject => rowObject.rateList.id == rateList.id);
      if (row) {
        row.rateList[stat] = row.rateList[stat] || 0;
        row.rateList[stat] += count;
      }
    }
  }
}
</script>
