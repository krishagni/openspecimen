<template>
  <os-button-group>
    <os-button left-icon="folder" :label="$t('carts.assign_folder')" right-icon="caret-down"
      @click="toggleFolderOptions" />
  </os-button-group>

  <os-overlay class="os-cart-folders" ref="folderOptions">
    <ul class="search">
      <li>
        <os-input-text v-model="searchTerm" :placeholder="$t('carts.search_folder')"
          @update:modelValue="searchFolders" />
      </li>
    </ul>
    <ul class="folders">
      <li v-for="(folder, idx) of folders" :key="idx" @click="addToFolder($event, folder)">
        <a> {{folder.name}} </a>
      </li>

      <li v-if="folders.length == 0">
        <a class="no-click" v-t="'carts.no_folders'"> No folders to show </a>
      </li>
    </ul>
    <ul class="actions">
      <li>
        <a @click="createNewFolder">
          <span v-t="'carts.create_new'">Create New </span>
        </a>
      </li>
      <li>
        <a @click="viewFolders" v-if="folders.length > 0">
          <span v-t="'carts.manage_folders'">Manage Folders </span>
        </a>
      </li>
    </ul>
  </os-overlay>
</template>

<script>

import alertsSvc from '@/common/services/Alerts.js';
import folderSvc from '@/biospecimen/services/SpecimenCartsFolder.js';
import itemsSvc  from '@/common/services/ItemsHolder.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['carts'],

  data() {
    return {
      searchTerm: '',

      folders: []
    }
  },

  methods: {
    toggleFolderOptions: function(event) {
      this.$refs.folderOptions.toggle(event);
      if (this.foldersLoaded) {
        return;
      }

      this.foldersLoaded = true;
      this.searchFolders(null);
    },

    searchFolders: function(searchTerm) {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
        this.searchTimer = null;
      }

      if (!searchTerm && this.defFolders) {
        this.folders = this.defFolders;
        return;
      }

      this.searchTimer = setTimeout(
        () => {
          if (this.defFolders && this.defFolders.length < 100) {
            searchTerm = searchTerm.toLowerCase();
            this.folders = this.defFolders.filter(folder => folder.name.toLowerCase().indexOf(searchTerm) != -1);
          } else {
            this.getFolders(searchTerm).then(
              folders => {
                this.folders = folders;
                if (!searchTerm) {
                  this.defFolders = folders;
                }
              }
            );
          }
        },
        searchTerm ? 500 : 0
      );
    },

    getFolders: function(searchTerm) {
      return folderSvc.getFolders({name: searchTerm});
    },

    addToFolder: function(event, folder) {
      if (event) {
        this.$refs.folderOptions.toggle(event);
      }

      if (!this.carts || this.carts.length == 0) {
        alertsSvc.error({code: 'carts.folder_min_one_cart_required'});
        // alertsSvc.error('Select at least one cart to add to the folder');
        return;
      }

      folderSvc.addCarts(folder, this.carts).then(
        ({count}) => {
          if (count == 0) {
            alertsSvc.info({code: 'carts.folder_no_carts_added', args: folder});
          } else {
            alertsSvc.info({code: 'carts.folder_carts_added', args: {count: count, name: folder.name}});
          }
        }
      );
    },

    createNewFolder: function(event) {
      this.$refs.folderOptions.toggle(event);
      if (!this.carts || this.carts.length == 0) {
        alertsSvc.error({code: 'carts.folder_min_one_cart_required'});
        return;
      }

      itemsSvc.setItems('carts', this.carts);
      routerSvc.goto('SpecimenCartsFolderAddEdit', {folderId: -1});
    },

    viewFolders: function() {
      routerSvc.goto('SpecimenCartsFoldersList');
    }
  }
}
</script>

<style scoped>
.os-cart-folders .os-input-text {
  padding: 0.5rem 1rem;
}

.os-cart-folders ul {
  margin: 0.5rem -1.25rem;
  padding: 0rem;
  padding-bottom: 0.5rem;
  list-style: none;
  border-bottom: 1px solid #ddd;
}

.os-cart-folders ul.carts {
  max-height: 190px;
  overflow: scroll;
}

.os-cart-folders ul:last-child {
  margin-bottom: 0rem;
  border-bottom: 0px;
}

.os-cart-folders ul li a {
  padding: 0.5rem 1rem;
  text-decoration: none;
  display: inline-block;
  width: 100%;
  color: #212529;
}

.os-cart-folders ul li a:hover:not(.no-click) {
  background: #e9ecef;
}

.os-cart-folders ul li a.no-click {
  cursor: initial;
}
</style>

<style>
.os-cart-folders .p-overlaypanel-content {
  padding: 0.5rem 1.25rem;
}
</style>
