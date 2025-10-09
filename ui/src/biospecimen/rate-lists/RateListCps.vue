<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="addCps" />
      </span>
    </template>
    <template #right>
      <os-list-size
        :list="ctx.cps"
        :page-size="ctx.pageSize"
        :list-size="ctx.cpsCount"
        @updateListSize="getCpsCount"
      />

      <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
    </template>
  </os-page-toolbar>
  <os-grid>
    <os-grid-column width="12">
      <os-list-view
        :data="ctx.cps"
        :schema="listSchema"
        :loading="ctx.loading"
        @filtersUpdated="loadCps"
        @rowClicked="onCpRowClick"
        ref="listView"
      />
    </os-grid-column>
  </os-grid>
</template>

<script>
import rateListSvc from '@/biospecimen/services/RateList.js';
import routerSvc   from '@/common/services/Router.js';

export default {
  props: ['rate-list'],

  data() {
    return {
      ctx: {
        cps: [],

        loading: false
      },

      cpUpdateOpts: {resource: 'CollectionProtocol', operations: ['Create', 'Update']},

      listSchema: rateListSvc.getCpListSchema()
    }
  },

  methods: {
    loadCps: function({filters, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;
      this._reloadCps();
    },

    getCpsCount: function() {
      const opts = Object.assign({}, this.ctx.filterValues || {});
      rateListSvc.getCollectionProtocolsCount(this.rateList.id, opts).then(({count}) => this.ctx.cpsCount = count);
    },

    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    onCpRowClick: function({cp}) {
      routerSvc.goto('CpDetail.Overview', {cpId: cp.id});
    },

    _reloadCps: function() {
      this.ctx.loading = true;
      const opts = Object.assign({maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      return rateListSvc.getCollectionProtocols(this.rateList.id, opts).then(
        resp => {
          this.ctx.loading = false;
          this.ctx.cps = resp.map(cp => ({cp}));
          return this.ctx.cps; 
        }
      );
    }
  }
}
</script>
