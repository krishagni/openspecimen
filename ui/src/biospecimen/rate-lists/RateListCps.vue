<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="showAddCpsDialog" />
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

    <os-dialog ref="addCpsDialog">
      <template #header>
        <span v-t="'lab_services.add_cps'">Add Collection Protocols</span>
      </template>
      <template #content>
        <os-form ref="addCpsForm" :schema="addCpsSchema" :data="ctx" />
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="hideAddCpsDialog" />

        <os-button primary :label="$t('common.buttons.add')" @click="addCps" />
      </template>
    </os-dialog>
  </os-grid>
</template>

<script>
import alertsSvc   from '@/common/services/Alerts.js';
import rateListSvc from '@/biospecimen/services/RateList.js';
import routerSvc   from '@/common/services/Router.js';

export default {
  props: ['rate-list'],

  emits: ['rate-list-cps-added'],

  data() {
    return {
      ctx: {
        cps: [],

        loading: false,

        cpsToAdd: []
      },

      cpUpdateOpts: {resource: 'CollectionProtocol', operations: ['Create', 'Update']},

      listSchema: rateListSvc.getCpsListSchema(),

      addCpsSchema: rateListSvc.getAddCpsSchema()
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

    showAddCpsDialog: function() {
      this.ctx.cpsToAdd = [];
      this.$refs.addCpsDialog.open();
    },

    hideAddCpsDialog: function() {
      this.ctx.cpsToAdd = [];
      this.$refs.addCpsDialog.close();
    },

    addCps: function() {
      const {cpsToAdd} = this.ctx;
      if (cpsToAdd.length == 0) {
        alertsSvc.error({code: 'lab_services.select_atleast_one_cp'});
        return;
      }

      rateListSvc.addCollectionProtocols(this.rateList.id, cpsToAdd).then(
        ({count}) => {
          alertsSvc.success({code: 'lab_services.cps_added', args: {count}});
          this.$refs.listView.reload();
          this.$emit('rate-list-cps-added', {count});
          this.hideAddCpsDialog();
        }
      );
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
