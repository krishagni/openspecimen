<template>
  <os-screen>
    <os-screen-panel :width="12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'cps.list'">Collection Protocols</h3>
          </span>

          <template #right>
            <os-list-size
              :list="ctx.listInfo.list"
              :page-size="ctx.listInfo.pageSize"
              :list-size="ctx.listInfo.size"
              @updateListSize="getListItemsCount"
            />
          </template>
        </os-page-head>
        <os-page-body v-if="ctx.inited">
          <os-page-toolbar>
            <template #default>
              <span v-if="!ctx.selectedItems || ctx.selectedItems.length == 0">
                <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="createCp"
                  v-show-if-allowed="cpResources.createOpts" />

                <os-button :label="$t('cpgs.list')" @click="viewCpGroupsList" />

                <os-menu :label="$t('common.buttons.more')" :options="moreMenuOpts" />
              </span>
              <span v-else>
                <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteCps"
                  v-show-if-allowed="cpResources.deleteOpts" />

                <os-menu :label="$t('common.buttons.export')" :options="exportOpts" />
              </span>
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-query-list-view
            name="cp-list-view"
            :object-id="-1"
            :query="ctx.query"
            :id-filter="'CollectionProtocol.id'"
            :auto-search-open="true"
            :allow-selection="true"
            :selected="ctx.selectedItem"
            :include-count="includeCount"
            :url="itemUrl"
            :newUiUrl="true"
            :newTab="false"
            :allowStarring="true"
            :showRowActions="true"
            @selectedRows="onItemSelection"
            @rowClicked="onItemRowClick"
            @rowStarToggled="onItemRowStarToggle"
            @listLoaded="onListLoad"
            ref="list">

            <template #actions="{rowObject}">
              <os-button-group>
                <os-button left-icon="eye" @click="viewCpDetails(rowObject)"
                  v-os-tooltip.bottom="$t('cps.view_details')" />

                <os-button left-icon="table" @click="viewCatalog(rowObject)"
                  v-os-tooltip.bottom="$t('cps.view_catalog')"
                  v-if="hasCatalog && queryReadAllowed && rowObject.hidden.catalogId > 0" />

                <os-button left-icon="chart-line" @click="viewDashboard(rowObject)"
                  v-os-tooltip.bottom="$t('cps.view_dashboard')"
                  v-if="hasDashboard && queryReadAllowed" />
              </os-button-group>
            </template>
          </os-query-list-view>
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-plugin-views ref="moreMenuPluginViews" page="cps-list" view="menu" :viewProps="ctx" />

    <os-confirm-delete ref="deleteCpsDialog" :captcha="false" :collectReason="true">
      <template #message>
        <span v-t="'cps.delete_selected'">Are you sure you want to delete the selected collection protocols along with their associated participants, visits and specimens data?</span>
      </template>
    </os-confirm-delete>

    <os-dialog ref="importCpDialog">
      <template #header>
        <span v-t="'cps.import_cp'">Import Collection Protocol</span>
      </template>
      <template #content>
        <os-form ref="cpJsonImporter" :schema="importCpFs" :data="{}" />
      </template>
      <template #footer>
        <os-button text    :label="$t('common.buttons.cancel')" @click="hideImportCpDialog" />
        <os-button primary :label="$t('common.buttons.import')" @click="uploadCpJson" />
      </template>
    </os-dialog>
  </os-screen>
</template>

<script>
import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';

import alertsSvc from '@/common/services/Alerts.js';
import authSvc   from '@/common/services/Authorization.js';
import exportSvc from '@/common/services/ExportService.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

import cpResources from './Resources.js';

import importCpSchema from '@/biospecimen/schemas/cps/import-cp-json.js';

export default {
  name: 'CpsList',

  inject: ['ui'],

  props: ['filters'],

  data() {
    const ui = this.ui;
    const ctx = {
      ui,

      inited: false,

      selectedItems: [],

      query: this.filters,

      listInfo: {
        list: [],
        pageSize: 0,
        size: 0
      },

      pluginMenuOptions: []
    };

    return {
      ctx,

      cpResources,

      importCpFs: importCpSchema.layout
    };
  },

  created() {
    let filters = {};
    const {query} = this.ctx;
    if (query) {
      filters = JSON.parse(decodeURIComponent(atob(query)));
    }

    if (Object.keys(filters).length > 0) {
      this.ctx.query = util.uriEncode(filters);
    }

    this.ctx.inited = true;
    util.getPluginMenuOptions(this.$refs.moreMenuPluginViews, 'cps-list', 'menu', this.ctx)
        .then(pluginOptions => this.ctx.pluginMenuOptions = pluginOptions || []);
  },

  watch: {
    '$route.query.filters': function(newValue) {
      this.ctx.query = newValue;
    }
  },

  computed: {
    itemUrl: function() {
      return "'#/cps/' + hidden.cpId + '/detail/overview'";
    },

    queryReadAllowed: function() {
      return this._isAllowed(['Query'], ['Read']);
    },

    hasCatalog: function() {
      return (this.$ui.global.appProps.plugins || []).indexOf('sc') != -1
    },

    hasDashboard: function() {
      return (this.$ui.global.appProps.plugins || []).indexOf('dashboard') != -1
    },

    exportOpts: function() {
      if (!this._isAllowed(['CollectionProtocol'], ['Export Import'])) {
        return [];
      }

      const options = [];
      options.push({
        icon: 'calendar',
        caption: this.$t('cps.list'),
        onSelect: () => this.exportCps()
      });

      options.push({
        icon: 'table',
        caption: this.$t('cps.events'),
        onSelect: () => this.exportCpes()
      });

      options.push({
        icon: 'flask',
        caption: this.$t('cps.srs'),
        onSelect: () => this.exportReqs()
      });

      return options;
    },

    moreMenuOpts: function() {
      const options = [];

      if (this._isCpImportAllowed()) {
        options.push({
          icon: 'upload',
          caption: this.$t('cps.import_cp_json'),
          onSelect: () => this.showImportCpDialog()
        });
      }

      if (this._isImportAllowed()) {
        options.push({
          icon: 'upload',
          caption: this.$t('cps.import_biospecimen_data'),
          onSelect: () => routerSvc.ngGoto('cps/import-multi-cp-objs')
        });
        options.push({
          icon: 'table',
          caption: this.$t('import.view_past_imports'),
          onSelect: () => routerSvc.ngGoto('cps/import-multi-cp-jobs')
        });
      }

      if (this._isExportAllowed()) {
        if (options.length > 0) {
          options.push({divider: true});
        }

        options.push({
          icon: 'download',
          caption: this.$t('common.buttons.export'),
          onSelect: () => routerSvc.ngGoto('cps/export-multi-cp-objs')
        });
      }

      if (options.length > 0 && this.ctx.pluginMenuOptions.length > 0) {
        options.push({divider: true});
        Array.prototype.push.apply(options, this.ctx.pluginMenuOptions);
      }

      return options;
    }
  },

  methods: {
    reloadList: function() {
      this.$refs.list.reload();
    },

    onItemSelection: function(items) {
      this.ctx.selectedItems = items.map(({rowObject}) => ({...rowObject.hidden, id: +rowObject.hidden.cpId}));
    },

    onItemRowClick: function(event) {
      const {cpId} = event.hidden || {};
      routerSvc.goto('ParticipantsList', {cpId, cprId: -1}, {});
    },

    viewCpDetails: function({hidden: {cpId}}) {
      routerSvc.goto('CpDetail.Overview', { cpId }, {filters: this.ctx.query});
    },

    viewCatalog: function({hidden: {cpId, catalogId}}) {
      routerSvc.ngGoto('s/specimen-catalogs/' + catalogId + '/dashboard?cpId=' + cpId);
    },

    viewDashboard: function({hidden: {cpId}}) {
      routerSvc.ngGoto('cp-view/' + cpId + '/dashboard');
    },

    onItemRowStarToggle: function(event) {
      const promise = event.starred ? cpSvc.unstarCp(event.hidden.cpId) : cpSvc.starCp(event.hidden.cpId);
      promise.then(() => event.starred = !event.starred);
    },

    getListItemsCount: function() {
      this.$refs.list.loadListSize().then(
        () => {
          this.ctx.listInfo.size = this.$refs.list.size;
        }
      );
    },

    onListLoad: function({widget, filters}) {
      const numFormatter         = Intl.NumberFormat().format;
      const participantsCountIdx = widget.list.columns.length;
      const specimensCountIdx    = participantsCountIdx + 1;

      // +1 for star
      widget.schema.columns[participantsCountIdx + 1].href = ({rowObject: {id}}) =>
        routerSvc.getUrl('ParticipantsList', {cpId: id, cprId: -1});
      widget.schema.columns[specimensCountIdx + 1].href = ({rowObject: {id}}) =>
        routerSvc.getUrl('ParticipantsList', {cpId: id, cprId: -1}, {view: 'specimens_list'});

      this.showListSize = true;
      this.ctx.selectedItems.length = 0;
      this.items = widget.data;
      this.items.forEach(
        row => {
          const hidden = row.hidden = row.hidden || {};
          row.id = hidden.cpId;

          const column = row.column;
          column['a_' + participantsCountIdx] = numFormatter(column['a_' + participantsCountIdx]);
          column['a_' + specimensCountIdx]    = numFormatter(column['a_' + specimensCountIdx]);
          
          if (column['a_' + participantsCountIdx] == -1) {
            column['a_' + participantsCountIdx] = 'N/A';
          }
        }
      );

      routerSvc.replace('CpsList', {}, { filters });
      setTimeout(() => {
        this.ctx.listInfo = {
          list: this.$refs.list.list.rows,
          size: this.$refs.list.size,
          pageSize: this.$refs.list.pageSize
        }
      }, 0);
    },

    openSearch: function () {
      this.$refs.list.toggleShowFilters();
    },

    createCp: function() {
      routerSvc.goto('CpAddEdit', {cpId: -1});
    },

    viewCpGroupsList: function() {
      routerSvc.ngGoto('cp-groups');
    },

    deleteCps: function() {
      this.$refs.deleteCpsDialog.open().then(
        ({reason}) => {
          const ids = this.ctx.selectedItems.map(({cpId}) => cpId);
          cpSvc.deleteCps(ids, reason).then(
            ({completed}) => {
              if (completed) {
                alertsSvc.success({code: 'cps.many_deleted', args: {count: ids.length}});
              } else {
                alertsSvc.info({code: 'cps.delete_more_time'});
              }

              this.reloadList();
            }
          );
        }
      );
    },

    exportCps: function() {
      this._exportRecords('cp');
    },

    exportCpes: function() {
      this._exportRecords('cpe');
    },

    exportReqs: function() {
      this._exportRecords('sr');
    },

    showImportCpDialog: function() {
      this.$refs.importCpDialog.open();
    },

    hideImportCpDialog: function() {
      this.$refs.importCpDialog.close();
    },

    uploadCpJson: function() {
      this.$refs.cpJsonImporter.uploadFile('file').then(
        savedCp => {
          this.$goto('CpDetail.Overview', {cpId: savedCp.id});
        }
      );
    },

    cpDictFn: function() {
      const dict = [];
      return async () => {
        if (dict.length > 0) {
          return dict;
        }

        Array.prototype.push.apply(dict, await cpSvc.getDict());
        return dict;
      }
    },

    _exportRecords: function(objectType) {
      var cpIds = this.ctx.selectedItems.map(({cpId}) => +cpId);
      exportSvc.exportRecords({objectType, recordIds: cpIds});
    },

    _isCpImportAllowed: function() {
      const resources = ['Collection Protocol'];
      return this._isAllowed(resources, ['Create']) && this._isAllowed(resources, ['Export Import']);
    },

    _isImportAllowed: function() {
      const resources       = ['ParticipantPhi', 'Visit', 'Specimen'];
      const createUpdateOps = ['Create', 'Update'];
      const eximOps         = ['Export Import'];

      return this._isAllowed(resources, createUpdateOps) && this._isAllowed(resources, eximOps);
    },

    _isExportAllowed: function() {
      const resources  = ['ParticipantPhi', 'ParticipantDeid', 'Visit', 'Specimen'];
      const readOps    = ['Read'];
      const eximOps    = ['Export Import'];

      return this._isAllowed(resources, readOps) && this._isAllowed(resources, eximOps);
    },

    _isAllowed: function(resources, operations) {
      return authSvc.isAllowed({resources, operations});
    }
  }
}
</script>
