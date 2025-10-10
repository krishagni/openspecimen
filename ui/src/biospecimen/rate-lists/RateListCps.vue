<template>
  <os-page-toolbar>
    <template #default>
      <span v-show-if-allowed="cpUpdateOpts">
        <os-button left-icon="plus" :label="$t('common.buttons.add')" @click="showAddCpsDialog"
          v-if="!ctx.selectedCps || ctx.selectedCps.length == 0" />
        <os-button left-icon="times" :label="$t('common.buttons.remove')" @click="removeCps" v-else />
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
        :allowSelection="true"
        @filtersUpdated="loadCps"
        @rowClicked="onCpRowClick"
        @selectedRows="onCpsSelection"
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

    <os-confirm ref="removeCpsConfirmDialog">
      <template #title>
        <span v-t="'lab_services.remove_cps'">Remove Collection Protocols</span>
      </template>
      <template #message>
        <span v-t="'lab_services.remove_cps_confirm'">Are you sure you want to remove association of the selected collection protocols with the rate list?</span>
      </template>
    </os-confirm>
  </os-grid>
</template>

<script>
import alertsSvc   from '@/common/services/Alerts.js';
import rateListSvc from '@/biospecimen/services/RateList.js';
import routerSvc   from '@/common/services/Router.js';

export default {
  props: ['rate-list'],

  emits: ['rate-list-cps-added', 'rate-list-cps-removed'],

  data() {
    return {
      ctx: {
        cps: [],

        loading: false,

        cpsToAdd: [],

        selectedCps: []
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

    onCpsSelection: function(selection) {
      this.ctx.selectedCps = (selection || []).map(({rowObject: {cp}}) => cp);
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

    removeCps: async function() {
      const resp = await this.$refs.removeCpsConfirmDialog.open();
      if (resp != 'proceed') {
        return;
      }

      const {selectedCps} = this.ctx;
      rateListSvc.removeCollectionProtocols(this.rateList.id, selectedCps).then(
        ({count}) => {
          alertsSvc.success({code: 'lab_services.cps_removed', args: {count}});
          this.$refs.listView.reload();
          this.$emit('rate-list-cps-removed', {count});
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
