<template>
  <os-page>
    <os-page-head>
      <span>
        <h3>Cart Folders</h3>
      </span>

      <template #right>
        <os-list-size
          :list="ctx.folders"
          :page-size="ctx.pageSize"
          :list-size="ctx.foldersCount"
          @updateListSize="getFoldersCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-button left-icon="shopping-cart" :label="$t('carts.view_carts')" @click="viewCarts" />
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.folders"
        :schema="listSchema"
        :query="ctx.query"
        :loading="ctx.loading"
        :showRowActions="true"
        @filtersUpdated="loadFolders"
        @rowClicked="onFolderRowClick"
        ref="listView">

        <template #rowActions="slotProps">
          <os-button-group v-if="slotProps.rowObject.editAllowed">
            <os-button size="small" left-icon="edit" v-os-tooltip.bottom="$t('common.buttons.edit')"
              @click="editFolder(slotProps.rowObject)" />
            <os-button size="small" left-icon="trash" v-os-tooltip.bottom="$t('common.buttons.delete')"
              @click="deleteFolder(slotProps.rowObject)" />
          </os-button-group>
        </template>
      </os-list-view>
    </os-page-body>
  </os-page>

  <os-confirm-delete ref="confirmDeleteFolder" :captcha="false">
    <template #message>
      <span v-t="{path: 'carts.confirm_folder_delete', args: ctx.toDelete}"> </span>
    </template>
  </os-confirm-delete>
</template>

<script>

import listSchema from  '@/biospecimen/schemas/cart-folders/list.js';

import alertSvc    from '@/common/services/Alerts.js';
import folderSvc   from '@/biospecimen/services/SpecimenCartsFolder.js';
import routerSvc   from '@/common/services/Router.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        folders: [],

        foldersCount: -1,

        loading: true,

        query: this.filters,
      },

      listSchema,
    };
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadFolders: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      await this.reloadFolders();
      routerSvc.goto('SpecimenCartsFoldersList', {}, {filters: uriEncoding});
    },

    reloadFolders: async function() {
      this.ctx.loading = true;

      const defOpts = {includeStats: true, maxResults: this.ctx.pageSize}; 
      const opts    = Object.assign(defOpts, this.ctx.filterValues || {});
      const folders = await folderSvc.getFolders(opts);

      this.ctx.loading = false;
      this.ctx.folders = folders.map(folder => ({folder: folder, editAllowed: this.isEditAllowed(folder)}));
      return this.ctx.folders;
    },

    isEditAllowed: function(folder) {
      return this.$ui.currentUser.admin || this.$ui.currentUser.id == folder.owner.id;   
    },

    getFoldersCount: function() {
      this.ctx.foldersCount = -1;
      const opts = Object.assign({}, this.ctx.filterValues);
      folderSvc.getFoldersCount(opts).then(({count}) => this.ctx.foldersCount = count);
    },

    onFolderRowClick: function({folder}) {
      routerSvc.goto('SpecimenCartsList', {cartId: -1}, {folderId: folder.id});
    },

    viewCarts: function() {
      routerSvc.goto('SpecimenCartsList', {cartId: -1}, {});
    },

    editFolder: function({folder}) {
      routerSvc.goto('SpecimenCartsFolderAddEdit', {folderId: folder.id});
    },

    deleteFolder: function({folder}) {
      const self = this;
      this.ctx.toDelete = folder;
      this.$refs.confirmDeleteFolder.open().then(
        function(resp) {
          if (resp != 'proceed') {
            return;
          }

          folderSvc.delete(folder).then(
            () => {
              alertSvc.success({code: 'carts.folder_deleted', args: folder});
              self.reloadFolders();
            }
          );
        }
      );
    }
  }
}
</script>
