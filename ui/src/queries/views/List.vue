<template>
  <os-page>
    <os-page-head>
      <h3>
        <span v-t="'queries.list'">Queries</span>
      </h3>

      <template #right>
        <os-list-size
          :list="ctx.queries"
          :page-size="ctx.pageSize"
          :list-size="ctx.queriesCount"
          @updateListSize="getQueriesCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <span v-if="!ctx.selectedQueries || ctx.selectedQueries.length == 0">
            <os-button left-icon="plus" :label="$t('common.buttons.create')" @click="createQuery"
              v-show-if-allowed="queryResources.createOpts" />

            <os-button left-icon="upload" :label="$t('common.buttons.import')" @click="showImportQueryDialog"
              v-show-if-allowed="queryResources.createOpts" />

            <os-button left-icon="list" :label="$t('queries.view_audit_logs')" @click="viewAuditLogs"
              v-show-if-allowed="'institute-admin'" />

            <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
              url="https://openspecimen.atlassian.net/l/cp/2AHMwCtu" :new-tab="true" />
          </span>
          <span v-else>
            <AssignFolder :my-folders="ctx.myFolders" :shared-folders="ctx.sharedFolders"
              @add-to-folder="addToFolder($event)" @create-new-folder="showCreateFolderDialog" />

            <os-button left-icon="times" :label="$t('queries.rm_from_folder')"
              @click="removeFromFolder" v-if="selectedFolder && selectedFolder.editable" />
          </span>
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-grid>
        <os-grid-column :width="3">
          <div class="folders">
            <div class="section-fixed">
              <div class="title">
                <span v-t="'queries.folders'">Folders</span>
              </div>
              <ul>
                <li :class="{selected: !folderId}" @click="selectFolder(null)">
                  <span v-t="'queries.my_queries'">My Queries</span>
                </li>
                <li :class="{selected: folderId == -1}" @click="selectFolder(-1)">
                  <span v-t="'queries.all_queries'">All Queries</span>
                </li>
              </ul>
            </div>
            <div class="section">
              <div class="title">
                <span v-t="'queries.my_folders'">My folders</span>
              </div>
              <ul>
                <li v-if="!ctx.myFolders || ctx.myFolders.length == 0">
                  <span v-t="'common.none'"></span>
                </li>

                <li :class="{selected: folderId == folder.id}" v-for="folder of ctx.myFolders" :key="folder.id">
                  <a @click="selectFolder(folder.id)">
                    <span>{{folder.name}}</span>
                  </a>
                  <os-button text left-icon="edit" @click="showUpdateFolderDialog(folder)" />
                </li>
              </ul>
            </div>
            <div class="section">
              <div class="title">
                <span v-t="'queries.shared_folders'">Shared folders</span>
              </div>
              <ul>
                <li :class="{selected: folderId == folder.id}" v-for="folder of ctx.sharedFolders" :key="folder.id">
                  <a @click="selectFolder(folder.id)">
                    <span>{{folder.name}}</span>
                  </a>
                  <os-button text left-icon="edit" @click="showUpdateFolderDialog(folder)" v-if="folder.editable" />
                  <os-button text left-icon="info-circle" @click="showFolderDetailsDialog(folder)" v-else />
                </li>
              </ul>
            </div>
          </div>
        </os-grid-column>

        <os-grid-column :width="9">
          <os-list-view
            :data="ctx.queries"
            :schema="listSchema"
            :query="ctx.query"
            :allowSelection="true"
            :loading="ctx.loading"
            :showRowActions="true"
            @filtersUpdated="loadQueries"
            @rowClicked="onQueryRowClick"
            @selectedRows="onQueriesSelection"
            @rowStarToggled="onToggleStar"
            ref="listView">

            <template #rowActions="{rowObject}">
              <os-menu icon="ellipsis-v" :options="queryOptions(rowObject.query)" />
            </template>
          </os-list-view>
        </os-grid-column>
      </os-grid>

      <os-confirm ref="deleteQueryDialog">
        <template #title>
          <span v-t="'queries.delete_query'">Delete Query</span>
        </template>
        <template #message>
          <span v-t="{path: 'queries.confirm_delete_query', args: ctx.query}"></span>
        </template>
      </os-confirm>

      <os-dialog ref="folderDetailsDialog">
        <template #header>
          <span v-t="'queries.folder_details'">Folder Details</span>
        </template>
        <template #content>
          <os-overview :schema="folderSchema.fields" :object="ctx" :columns="1" :bg-col="true" />
        </template>
        <template #footer>
          <os-button primary :label="$t('common.buttons.done')" @click="hideFolderDetailsDialog" />
        </template>
      </os-dialog>

      <os-dialog ref="updateFolderDialog">
        <template #header>
          <span v-t="'queries.update_folder'" v-if="ctx.folder.id > 0">Update Folder</span>
          <span v-t="'queries.create_folder'" v-else>Create Folder</span>
        </template>
        <template #content>
          <os-form ref="folderForm" :schema="folderAddEditFs" :data="ctx" />
        </template>
        <template #footer>
          <os-button text    :label="$t('common.buttons.cancel')" @click="hideUpdateFolderDialog" />
          <os-button danger  :label="$t('common.buttons.delete')" @click="deleteFolder" v-if="ctx.folder.id > 0"/>
          <os-button primary :label="$t('common.buttons.update')" @click="updateFolder" v-if="ctx.folder.id > 0"/>
          <os-button primary :label="$t('common.buttons.create')" @click="updateFolder" v-else />
        </template>
      </os-dialog>

      <os-confirm ref="deleteFolderDialog">
        <template #title>
          <span v-t="'queries.delete_folder'">Delete Folder</span>
        </template>
        <template #message>
          <span v-t="{path: 'queries.confirm_delete_folder', args: ctx.folder}"></span>
        </template>
      </os-confirm>

      <os-dialog ref="importQueryDialog">
        <template #header>
          <span v-t="'queries.import_query'">Import Query</span>
        </template>
        <template #content>
          <os-form ref="queryImporter" :schema="importQueryFs" :data="{}" />
        </template>
        <template #footer>
          <os-button text    :label="$t('common.buttons.cancel')" @click="hideImportQueryDialog" />
          <os-button primary :label="$t('common.buttons.import')" @click="uploadQueryJson" />
        </template>
      </os-dialog>
    </os-page-body>
  </os-page>
</template>

<script>

import folderAddEditLayout from '@/queries/schemas/addedit-folder.js';
import folderSchema        from '@/queries/schemas/folder.js';
import importSchema        from '@/queries/schemas/import-query.js';
import listSchema          from '@/queries/schemas/list.js';

import alertsSvc      from '@/common/services/Alerts.js';
import authSvc        from '@/common/services/Authorization.js';
import formUtil       from '@/common/services/FormUtil.js';
import queryFolderSvc from '@/queries/services/QueryFolder.js';
import routerSvc      from '@/common/services/Router.js';
import savedQuerySvc  from '@/queries/services/SavedQuery.js';

import AssignFolder   from './AssignFolder.vue';
import queryResources from './Resources.js';

export default {
  props: ['filters', 'folderId'],

  components: {
    AssignFolder
  },

  data() {
    const folderAddEditFs = formUtil.getFormSchema(folderSchema.fields, folderAddEditLayout.layout);

    return {
      ctx: {
        queries: [],

        queriesCount: -1,

        loading: true,

        query: this.filters,

        selectedQueries: [],

        myFolders: [],

        sharedFolders: []
      },

      listSchema,

      importQueryFs: importSchema.layout,

      folderSchema,

      folderAddEditFs,

      queryResources
    };
  },

  created() {
    this._loadFolders();
  },

  watch: {
    folderId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.$refs.listView.reload();
      }
    }
  },

  computed: {
    selectedFolder: function() {
      if (!this.folderId || this.folderId == -1) {
        return null;
      }

      let folder = (this.ctx.myFolders || []).find(folder => folder.id == this.folderId);
      if (!folder) {
        folder = (this.ctx.sharedFolders || []).find(folder => folder.id == this.folderId);
      }

      return folder;
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadQueries: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      await this.reloadQueries();
      routerSvc.goto('QueriesList', {}, {filters: uriEncoding, folderId: this.folderId});
    },

    reloadQueries: async function() {
      this.ctx.loading = true;
      const opts = {orderByStarred: true, returnList: true, folderId: this.folderId, maxResults: this.ctx.pageSize};
      const queries = await savedQuerySvc.getQueries(Object.assign(opts, this.ctx.filterValues || {}));

      this.ctx.queries = queries.map(query => ({ query }));
      this.ctx.loading = false;
      this.ctx.selectedQueries.length = 0;
      return this.ctx.queries;
    },

    getQueriesCount: async function() {
      const opts = Object.assign({folderId: this.folderId}, this.ctx.filterValues);
      const resp = await savedQuerySvc.getQueriesCount(opts);
      this.ctx.queriesCount = resp.count;
    },

    queryOptions: function(query) {
      const options = [];
      if (authSvc.isAllowed(queryResources.updateOpts)) {
        options.push({
          icon: 'edit',
          caption: this.$t('common.buttons.edit'),
          onSelect: () => this._editQuery(query)
        });
      }

      options.push({
        icon: 'download',
        caption: this.$t('common.buttons.download'),
        onSelect: () => this._downloadQuery(query)
      });
        
      if (authSvc.isAllowed({resource: 'ScheduledJob', operations: ['Create']})) {
        options.push({
          icon: 'calendar',
          caption: this.$t('queries.schedule'),
          onSelect: () => this._scheduleQuery(query)
        });
      }

      if (authSvc.isAllowed(queryResources.deleteOpts)) {
        options.push({
          icon: 'trash',
          caption: this.$t('common.buttons.delete'),
          onSelect: () => this._deleteQuery(query)
        });
      }

      return options;
    },

    onQueryRowClick: function({query}) {
      routerSvc.goto('QueryDetail.Results', {queryId: query.id});
    },

    onQueriesSelection: function(selection) {
      this.ctx.selectedQueries = selection;
    },

    onToggleStar: async function({query}) {
      let resp;
      if (query.starred) {
        resp = await savedQuerySvc.unstar(query.id);
      } else {
        resp = await savedQuerySvc.star(query.id);
      }

      if (resp.status) {
        query.starred = !query.starred;
      }
    },

    selectFolder: function(folderId) {
      routerSvc.goto('QueriesList', {}, {filters: this.filters, folderId});
    },

    showUpdateFolderDialog: function(folder) {
      this.ctx.folder = {};
      queryFolderSvc.getFolderById(folder.id, true).then(
        dbFolder => {
          this.ctx.folder = dbFolder;
          this.$refs.updateFolderDialog.open();
        }
      );
    },

    hideUpdateFolderDialog: function() {
      this.ctx.folder = null;
      this.$refs.updateFolderDialog.close();
    },

    updateFolder: function() {
      if (!this.$refs.folderForm.validate()) {
        return;
      }

      queryFolderSvc.saveOrUpdate(this.ctx.folder).then(
        savedFolder => {
          const msg = this.ctx.folder.id > 0 ? 'queries.folder_updated' : 'queries.folder_created';
          alertsSvc.success({code: msg, args: savedFolder});
          this._loadFolders();
          this.hideUpdateFolderDialog();
        }
      );
    },

    deleteFolder: function() {
      this.$refs.deleteFolderDialog.open().then(
        resp => {
          if (resp != 'proceed') {
            return;
          }

          queryFolderSvc.deleteFolder(this.ctx.folder).then(
            () => {
              alertsSvc.success({code: 'queries.folder_deleted', args: this.ctx.folder});
              if (this.folderId == this.ctx.folder.id) {
                this.selectFolder(null);
              }

              this._loadFolders();
              this.hideUpdateFolderDialog();
            }
          );
        }
      );
    },

    showFolderDetailsDialog: function(folder) {
      this.ctx.folder = {};
      queryFolderSvc.getFolderById(folder.id, false).then(
        dbFolder => {
          this.ctx.folder = dbFolder;
          this.$refs.folderDetailsDialog.open();
        }
      );
    },

    hideFolderDetailsDialog: function() {
      this.ctx.folder = {};
      this.$refs.folderDetailsDialog.close();
    },

    addToFolder: function(folder) {
      const queries = this.ctx.selectedQueries.map(({rowObject: {query}}) => query);
      queryFolderSvc.addQueriesToFolder(folder, queries).then(
        () => alertsSvc.success({code: 'queries.added_to_folder', args: folder})
      );
    },

    removeFromFolder: function() {
      const queries = this.ctx.selectedQueries.map(({rowObject: {query}}) => query);
      queryFolderSvc.rmQueriesFromFolder(this.selectedFolder, queries).then(
        () => {
          alertsSvc.success({code: 'queries.removed_from_folder', args: this.selectedFolder});
          this.reloadQueries();
        }
      );
    },

    showCreateFolderDialog: function() {
      const queries = this.ctx.selectedQueries.map(({rowObject: {query}}) => query);
      this.ctx.folder = {queries};
      this.$refs.updateFolderDialog.open();
    },

    createQuery: function() {
      routerSvc.goto('QueryDetail.AddEdit', {queryId: -1});
    },

    showImportQueryDialog: function() {
      this.$refs.importQueryDialog.open();
    },

    hideImportQueryDialog: function() {
      this.$refs.importQueryDialog.close();
    },

    uploadQueryJson: function() {
      this.$refs.queryImporter.uploadFile('file').then(
        savedQuery => {
          if (!savedQuery) {
            return;
          }

          alertsSvc.success({code: 'queries.saved', args: savedQuery});
          routerSvc.goto('QueryDetail.AddEdit', {queryId: savedQuery.id});
        }
      );
    },

    viewAuditLogs: function() {
      routerSvc.goto('QueryAuditLogs');
    },

    _editQuery: function(query) {
      routerSvc.goto('QueryDetail.AddEdit', {queryId: query.id});
    },

    _downloadQuery: function(query) {
      savedQuerySvc.downloadQuery(query.id);
    },

    _scheduleQuery: function(query) {
      routerSvc.goto('JobAddEdit', {jobId: -1}, {queryId: query.id});
    },

    _deleteQuery: function(query) {
      this.ctx.query = query;
      this.$refs.deleteQueryDialog.open().then(
        (resp) => {
          if (resp != 'proceed') {
            return;
          }

          savedQuerySvc.deleteQuery(query.id).then(
            () => {
              alertsSvc.success({code: 'queries.deleted', args: query});
              this.reloadQueries();
            }
          );
        }
      );
    },

    _loadFolders: async function() {
      const folders = await queryFolderSvc.getFolders();
      const myFolders = [], sharedFolders = [];
      for (const folder of folders) {
        if (folder.owner.id == this.$ui.currentUser.id) {
          folder.editable = true;
          myFolders.push(folder);
        } else {
          if (folder.owner.loginName != '$system') {
            const {admin, instituteAdmin, instituteId} = this.$ui.currentUser;
            folder.editable = admin ||
              (instituteAdmin && instituteId == folder.owner.instituteId) ||
              folder.allowEditsBySharedUsers;
          }

          sharedFolders.push(folder);
        }
      }

      this.ctx.myFolders = myFolders;
      this.ctx.sharedFolders = sharedFolders;
    }
  }
}
</script>

<style scoped>
.folders {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.folders .section {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.folders .section .title,
.folders .section-fixed .title {
  font-weight: bold;
}

.folders .section ul,
.folders .section-fixed ul {
  list-style: none;
  padding: 0;
  margin: 0;
  margin-bottom: 1rem;
  overflow-y: auto;
}

.folders .section ul li,
.folders .section-fixed ul li {
  padding: 0.5rem;
  border-bottom: 1px solid #ddd;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.folders .section ul li a,
.folders .section-fixed ul li a {
  flex: 1;
  text-decoration: none;
  color: inherit;
}

.folders .section ul li :deep(.btn.text.icon-btn),
.folders .section-fixed ul li :deep(.btn.text.icon-btn) {
  color: inherit;
  opacity: 0.8;
}

.folders .section ul li :deep(.btn.text.icon-btn:hover),
.folders .section-fixed ul li :deep(.btn.text.icon-btn:hover) {
  padding-left: 6px;
  padding-top: 2px;
  border-color: #ddd;
}

.folders .section ul li:last-child,
.folders .section-fixed ul li:last-child {
  border-bottom: 0;
}

.folders .section ul li.selected a,
.folders .section-fixed ul li.selected a {
  color: #dd4b39;
}

.folders .section ul li.selected:before,
.folders .section-fixed ul li.selected:before {
  border-left: 0.25rem solid #dd4b39;
  display: block;
  content: ' ';
  position: absolute;
  height: 1.25rem;
  left: 1rem;
}
</style>
