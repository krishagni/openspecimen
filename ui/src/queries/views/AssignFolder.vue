<template>
  <os-button-group>
    <os-button left-icon="folder" :label="$t('queries.assign')" right-icon="caret-down" @click="toggleFolderOptions" />
  </os-button-group>

  <os-overlay class="os-query-folders" ref="folderOptions">
    <ul class="search">
      <li>
        <os-input-text v-model="searchTerm" :placeholder="$t('queries.search_folder')"
          @update:modelValue="searchFolders" />
      </li>
    </ul>
    <ul class="folders" v-if="myFolderOptions.length > 0">
      <li v-for="folder of myFolderOptions" :key="folder.id" @click="addToFolder($event, folder)">
        <a> {{folder.name}} </a>
      </li>
    </ul>
    <ul class="folders" v-if="sharedFolderOptions.length > 0">
      <li v-for="folder of sharedFolderOptions" :key="folder.id" @click="addToFolder($event, folder)">
        <a> {{folder.name}} </a>
      </li>
    </ul>
    <ul class="folders" v-if="myFolderOptions.length == 0 && sharedFolderOptions.length == 0">
      <li>
        <a v-t="'queries.no_folders'"> No folders to show </a>
      </li>
    </ul>
    <ul class="actions">
      <li>
        <a @click="createNewFolder">
          <span v-t="'queries.create_folder'">Create New Folder</span>
        </a>
      </li>
    </ul>
  </os-overlay>
</template>

<script>

export default {
  props: ['my-folders', 'shared-folders'],

  emits: ['add-to-folder', 'create-new-folder'],

  data() {
    return {
      searchTerm: ''
    }
  },

  computed: {
    myFolderOptions: function() {
      const myFolders = this.myFolders || [];
      return !this.searchTerm ? myFolders : this._filterFolders(myFolders, this.searchTerm);
    },

    sharedFolderOptions: function() {
      const sharedFolders = (this.sharedFolders || []).filter(folder => folder.editable);
      return !this.searchTerm ? sharedFolders : this._filterFolders(sharedFolders, this.searchTerm);
    },

  },

  methods: {
    toggleFolderOptions: function(event) {
      this.searchTerm = null;
      this.$refs.folderOptions.toggle(event);
    },

    addToFolder: function(event, folder) {
      if (event) {
        this.$refs.folderOptions.toggle(event);
      }

      this.$emit('add-to-folder', folder);
    },

    createNewFolder: function(event) {
      this.$refs.folderOptions.toggle(event);
      this.$emit('create-new-folder');
    },

    _filterFolders: function(folders, searchTerm) {
      searchTerm = searchTerm.toLowerCase();
      return folders.filter(folder => folder.name.toLowerCase().indexOf(searchTerm) >= 0);
    }
  }
}
</script>

<style scoped>
.os-query-folders .os-input-text {
  padding: 0.5rem 1rem;
}

.os-query-folders ul {
  margin: 0.5rem -1.25rem;
  padding: 0rem;
  padding-bottom: 0.5rem;
  list-style: none;
  border-bottom: 1px solid #ddd;
}

.os-query-folders ul.folders {
  max-height: 190px;
  overflow: scroll;
}

.os-query-folders ul:last-child {
  margin-bottom: 0rem;
  border-bottom: 0px;
}

.os-query-folders ul li a {
  padding: 0.5rem 1rem;
  text-decoration: none;
  display: inline-block;
  width: 100%;
  color: #212529;
}

.os-query-folders ul li a:hover:not(.no-click) {
  background: #e9ecef;
}

.os-query-folders ul li a.no-click {
  cursor: initial;
}
</style>

<style>
.os-query-folders .p-overlaypanel-content {
  padding: 0.5rem 1.25rem;
}
</style>
