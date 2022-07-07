<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3>Container Types</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="'Switch to table view'"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.types"
              :page-size="ctx.pageSize"
              :list-size="ctx.typesCount"
              @updateListSize="getTypesCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default v-if="ctx.selectedTypes && ctx.selectedTypes.length > 0">
              <os-button left-icon="trash" label="Delete" @click="confirmDelete"
                v-show-if-allowed="'institute-admin'" />

              <os-button left-icon="download" label="Export" @click="exportTypes"
                v-show-if-allowed="typeResources.importOpts" />
            </template>

            <template #default v-else>
              <os-button left-icon="plus" label="Create" @click="createType"
                v-show-if-allowed="'institute-admin'" />

              <os-button left-icon="box-open" label="Containers" @click="viewContainers" />

              <os-menu label="Import" :options="importOpts"
                v-show-if-allowed="'institute-admin'" />

              <os-button left-icon="download" label="Export" @click="exportTypes"
                v-show-if-allowed="typeResources.importOpts" />

              <os-button-link left-icon="question-circle" label="Help"
                url="https://openspecimen.atlassian.net/wiki/x/ioDIBg" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" label="Search" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.types"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="true"
            :loading="ctx.loading"
            :selected="ctx.selectedType"
            @filtersUpdated="loadTypes"
            @rowClicked="onTypeRowClick"
            @selectedRows="onTypesSelection"
            ref="listView"
          />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.typeId > 0 && ctx.selectedType">
      <router-view :typeId="ctx.selectedType.type.id" :key="$route.params.typeId" />
    </os-screen-panel>
  </os-screen>


  <os-confirm-delete :captcha="true" ref="confirmDeleteDialog">
    <template #message>
      <span>Are you sure you want to delete the selected container types?</span>
    </template>
  </os-confirm-delete>
</template>

<script>

import listSchema from '@/administrative/schemas/container-types/list.js';

import typesSvc   from '@/administrative/services/ContainerType.js';
import alertsSvc  from '@/common/services/Alerts.js';
import exportSvc  from '@/common/services/ExportService.js';
import routerSvc  from '@/common/services/Router.js';

import typeResources from './Resources.js';

export default {
  props: ['typeId', 'filters'],

  data() {
    return {
      ctx: {
        types: [],
        typesCount: -1,
        loading: true,
        query: this.filters,
        selectedTypes: [],
        selectedType: null,
        detailView: false
      },

      listSchema,

      importOpts: [
        {
          icon: 'cubes',
          caption: 'Types',
          onSelect: () => routerSvc.ngGoto('container-types-import')
        },
        {
          icon: 'table',
          caption: 'View Past Imports',
          onSelect: () => routerSvc.ngGoto('container-types-import-jobs')
        }
      ],

      typeResources
    };
  },

  watch: {
    'typeId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.ctx.types.find(rowObject => rowObject.type.id == this.typeId);
        if (!selectedRow) {
          selectedRow = {type: {id: this.typeId}};
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

    loadTypes: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      const types = await this.reloadTypes();
      if (this.typeId <= 0) {
        routerSvc.goto('ContainerTypesList', {typeId: -1}, {filters: uriEncoding});
      } else {
        let selectedRow = types.find(rowObject => rowObject.type.id == this.typeId);
        if (!selectedRow) {
          selectedRow = {type: {id: this.typeId}};
        }

        this.showDetails(selectedRow);
      }
    },

    reloadTypes: async function() {
      this.ctx.loading = true;
      const opts = {maxResults: this.ctx.pageSize};
      const types = await typesSvc.getTypes(Object.assign(opts, this.ctx.filterValues || {}));
      this.ctx.types = types.map(type => ({ type }));
      this.ctx.loading = false;
      this.ctx.selectedTypes = [];
      return this.ctx.types;
    },

    getTypesCount: async function() {
      const { count } = await typesSvc.getTypesCount({...this.ctx.filterValues});
      this.ctx.typesCount = count;
    },

    onTypeRowClick: function({type}) {
      routerSvc.goto('ContainerTypesListItemDetail.Overview', {typeId: type.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedType = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('ContainerTypesList', {typeId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    onTypesSelection: function(selection) {
      this.ctx.selectedTypes = selection;
    },

    createType: function() {
      routerSvc.goto('ContainerTypeAddEdit', {typeId: -1})
    },

    viewContainers: function() {
      routerSvc.goto('ContainersList');
    },

    confirmDelete: function() {
      this.$refs.confirmDeleteDialog.open().then(
        (resp) => {
          if (resp != 'proceed') {
            return;
          }

          const typeIds = this.ctx.selectedTypes.map(item => item.rowObject.type.id);
          typesSvc.bulkDelete(typeIds).then(
            () => {
              alertsSvc.success('Container types deleted successfully!');
              this.$refs.listView.reload();
            }
          );
        }
      );
    },

    exportTypes: function() {
      const typeIds = this.ctx.selectedTypes.map(item => item.rowObject.type.id);
      exportSvc.exportRecords({objectType: 'storageContainerType', recordIds: typeIds});
    }
  }
}
</script>
