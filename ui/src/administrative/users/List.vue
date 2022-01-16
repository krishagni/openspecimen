<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <template #breadcrumb v-if="ctx.group && !ctx.detailView">
            <os-breadcrumb :items="ctx.ugCrumb" />
          </template>

          <span>
            <h3 v-if="!ctx.group">Users</h3>
            <h3 v-else>{{ctx.group.name}}</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="'Switch to table view'"
              @click="showTable"
            />

            <os-list-size v-else
              :list="ctx.users"
              :page-size="ctx.pageSize"
              :list-size="ctx.usersCount"
              @updateListSize="getUsersCount"
            />
          </template>
        </os-page-head>
        <os-page-body v-if="ctx.inited">
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <span v-if="ctx.selectedUsers.length == 0 && !ctx.group">
                <os-button left-icon="plus" label="Create"
                  @click="goto('UserAddEdit', {userId: -1})"
                  v-show-if-allowed="userResources.createOpts" />

                <os-button left-icon="users" label="User Groups" @click="goto('UserGroupsList')" />

                <os-menu label="Import" :options="importOpts" v-show-if-allowed="userResources.importOpts" />

                <os-menu label="Export" :options="exportOpts" v-show-if-allowed="userResources.importOpts"/>

                <os-menu label="More" :options="moreOpts" v-show-if-allowed="institute" />

                <os-button left-icon="question-circle" label="Help" @click="help" />
              </span>

              <span v-if="ctx.selectedUsers.length > 0">
                <os-button left-icon="edit" label="Edit" @click="bulkEdit" v-show-if-allowed="userResources.updateOpts" />

                <AssignGroup v-if="!ctx.group" @addToGroup="addToGroup"
                  v-show-if-allowed="userResources.updateOpts" />

                <os-button v-if="ctx.group" left-icon="times" label="Remove from Group" @click="removeFromGroup"
                  v-show-if-allowed="userResources.updateOpts" />

                <os-button left-icon="archive" label="Archive" @click="archiveUsers"
                  v-show-if-allowed="userResources.updateOpts" />

                <os-button left-icon="check" label="Reactivate" @click="reactivateUsers"
                  v-show-if-allowed="userResources.updateOpts" />

                <os-button left-icon="trash" label="Delete" @click="deleteUsers"
                  v-show-if-allowed="userResources.deleteOpts" />

                <os-button left-icon="lock" label="Lock" @click="lockUsers"
                  v-show-if-allowed="userResources.updateOpts" />

                <os-button left-icon="unlock" label="Unlock" @click="unlockUsers"
                  v-show-if-allowed="userResources.updateOpts" />

                <os-button left-icon="thumbs-up" label="Approve" @click="approveUsers"
                  v-show-if-allowed="userResources.updateOpts" />

                <os-menu label="Export" :options="exportOpts" v-show-if-allowed="userResources.importOpts" />
              </span>
            </template>

            <template #right>
              <os-button left-icon="search" label="Search" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :data="ctx.users"
            :schema="ctx.listSchema"
            :query="ctx.query"
            :allowSelection="true"
            :loading="ctx.loading"
            :selected="ctx.selectedUser"
            @filtersUpdated="loadUsers"
            @selectedRows="onUsersSelection"
            @rowClicked="onUserRowClick"
            ref="listView"
          />

          <os-confirm-delete ref="deleteDialog">
            <template #message>
              <span>Are you sure you want to delete the selected users?</span>
            </template>
          </os-confirm-delete>

          <Announcement ref="announcementDialog" />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.userId > 0 && ctx.selectedUser">
      <router-view :userId="ctx.selectedUser.user.id" :listDetailView="true" :key="$route.params.userId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>
import { reactive } from 'vue';

import userGrpSvc from '@/administrative/services/UserGroup.js';
import userSvc from '@/administrative/services/User.js';

import alertSvc from '@/common/services/Alerts.js';
import authSvc from '@/common/services/Authorization.js';
import exportSvc from '@/common/services/ExportService.js';
import itemsSvc from '@/common/services/ItemsHolder.js';
import routerSvc from '@/common/services/Router.js';
import userGroupSvc from '@/administrative/services/UserGroup.js';

import AssignGroup from '@/administrative/user-groups/AssignGroup.vue';

import userResources from './Resources.js';
import Announcement from './Announcement.vue';

export default {
  name: 'UsersList',

  props: ['filters', 'userId', 'groupId'],

  components: {
    AssignGroup,
    Announcement
  },

  setup(props) {
    let ctx = reactive({
      inited: false,

      group: undefined,

      detailView: false,

      listSchema: { columns: [] },

      users: [],

      selectedUsers: [],

      query: props.filters,

      pageSize: undefined,

      usersCount: -1,

      ugCrumb: [ { url: routerSvc.getUrl('UserGroupsList'), label: 'User Groups' } ]
    });

    if (props.groupId) {
      //
      // remove institute and group filters
      //
      // ctx.filters.splice(2, 2);

      userGrpSvc.getGroup(props.groupId)
        .then(group => {
          ctx.group = group;
          ctx.inited = true;
        });
    } else {
      ctx.inited = true;
    }

    return {
      ctx,

      userResources
    };
  },

  created() {
    userSvc.getListViewSchema().then(
      listSchema => {
        let ls = this.ctx.listSchema = {...listSchema};
        if (this.groupId) {
          ls.filters = ls.filters.filter(f => f.name != 'institute' && f.name != 'group');
        }
      }
    );
  },

  watch: {
    'userId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        let selectedRow = this.ctx.users.find(rowObject => rowObject.user.id == this.userId);
        if (!selectedRow) {
          selectedRow = {user: {id: this.userId}};
        }
        this.showDetails(selectedRow);
      } else {
        this.showTable(newValue == -2);
      }
    }
  },

  methods: {
    openSearch: function () {
      this.$refs.listView.toggleShowFilters();
    },

    loadUsers: async function ({filters, uriEncoding, pageSize}) {
      this.ctx.loading = true;
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;

      let params = {filters: uriEncoding};
      if (this.ctx.group) {
        params.groupId = this.ctx.group.id;
      }

      let opts = Object.assign({maxResults: pageSize}, filters);
      if (this.ctx.group) {
        opts.group = this.ctx.group.name;
      }

      if (!this.$ui.currentUser.admin) {
        opts.institute = this.$ui.currentUser.instituteName;
      }

      const users = await userSvc.getUsers(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.users = resp.map(user => ({user: user}));
        return this.ctx.users;
      });

      if (this.userId <= 0) {
        this.ctx.selectedUser = null;
        routerSvc.goto('UsersList', {userId: -1}, params);
      } else {
        let selectedRow = users.find(rowObject => rowObject.user.id == this.userId);
        if (!selectedRow) {
          selectedRow = {user: {id: this.userId}};
        }

        this.showDetails(selectedRow);
      }
    },

    getUsersCount: function() {
      this.ctx.usersCount = -1;

      let opts = Object.assign({}, this.ctx.filterValues);
      if (this.ctx.group) {
        opts.group = this.ctx.group.name;
      }
      userSvc.getUsersCount(opts).then(resp => this.ctx.usersCount = resp.count);
    },

    onUsersSelection: function(selection) {
      this.ctx.selectedUsers = selection;
    },

    onUserRowClick: function(rowObject) {
      routerSvc.goto(
        'UsersListItemDetail.Overview',
        {userId: rowObject.user.id},
        {filters: this.filters, groupId: this.groupId}
      );
    },

    showDetails: function(rowObject) {
      this.ctx.selectedUser = rowObject;
      if (!this.ctx.detailView) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('UsersList', {userId: -1}, {filters: this.filters, groupId: this.groupId});
      if (reload) {
        this.$refs.listView.reload();
      }
    },

    bulkEdit: function() {
      const users = this.ctx.selectedUsers.map(item => ({id: item.rowObject.user.id}));
      itemsSvc.setItems('users', users);
      routerSvc.goto('UserBulkEdit');
    },

    updateStatus: function(fromStatuses, toStatus, msg) {
      const users = this.ctx.selectedUsers
        .map(item => item.rowObject.user)
        .filter(user => !fromStatuses || fromStatuses.length == 0 || fromStatuses.indexOf(user.activityStatus) != -1);

      const usersMap = {};
      users.forEach(u => usersMap[u.id] = u);

      let self = this;
      userSvc.bulkUpdate({detail: {activityStatus: toStatus}, ids: Object.keys(usersMap)}).then(
        function(saved) {
          alertSvc.success(saved.length + (saved.length != 1 ? ' users ' : ' user ') + msg);
          self.$refs.listView.reload();
        }
      );
    },

    addToGroup: function(group) {
      const users = this.ctx.selectedUsers.map(item => item.rowObject.user);
      if (users.length == 0) {
        return;
      }

      const instituteId = users[0].instituteId;
      for (let user of users) {
        if (user.instituteId != instituteId) {
          alertSvc.error('Users of multiple institutes cannot be added to the group.');
          return;
        }
      }

      if (group) {
        userGroupSvc.addUsers(group, users).then(() => alertSvc.success('Users added to the group ' + group.name));
      } else {
        itemsSvc.setItems('users', users);
        routerSvc.goto('UserGroupAddEdit', {groupId: -1});
      }
    },

    removeFromGroup: function() {
      const users = this.ctx.selectedUsers.map(item => item.rowObject.user);
      if (!users || users.length == 0) {
        return;
      }

      const self = this;
      userGrpSvc.removeUsers(this.ctx.group, users).then(
        function() {
          alertSvc.success('Users removed from the group!');
          self.$refs.listView.reload();
        }
      );
    },

    archiveUsers: function() {
      this.updateStatus(['Locked', 'Active', 'Expired'], 'Closed', 'archived');
    },

    reactivateUsers: function() {
      this.updateStatus(['Closed'], 'Active', 'reactivated');
    },

    lockUsers: function() {
      this.updateStatus(['Active'], 'Locked', 'locked');
    },

    unlockUsers: function() {
      this.updateStatus(['Locked'], 'Active', 'unlocked');
    },

    approveUsers: function() {
      this.updateStatus(['Pending'], 'Active', 'sign-up request approved');
    },

    deleteUsers: function() {
      const users = this.ctx.selectedUsers.map(item => item.rowObject.user);

      if (!this.$ui.currentUser.admin) {
        const admins = users.filter(user => user.admin == true)
          .map(user => user.firstName + ' ' + user.lastName)
          .join(',');

        if (admins.length > 0) {
          alertSvc.error('Super administrator rights required to delete admin users: ' + admins);
          return;
        }
      }

      this.$refs.deleteDialog.open().then(() => this.updateStatus([], 'Disabled', 'deleted'));
    },

    exportRecords: function(type) {
      const userIds = this.ctx.selectedUsers.map(item => item.rowObject.user.id);
      exportSvc.exportRecords({objectType: type, recordIds: userIds});
    },

    exportForms: function() {
      const users = this.ctx.selectedUsers.map(item => ({emailAddress: item.rowObject.user.emailAddress}));
      if (users.length > 0) {
        localStorage.setItem('os.users', JSON.stringify(users));
      }

      routerSvc.ngGoto('users-export-forms');
    },

    ngGoto: (url, params) => routerSvc.ngGoto(url, params),

    goto: (name, params) => routerSvc.goto(name, params),

    help: function() {
      window.open('http://help.openspecimen.org/user', '_blank').focus();
    },
  },

  computed: {
    importOpts: function() {
      return [
        { icon: 'user', caption: 'Users', onSelect: () => this.ngGoto('users-import', {objectType: 'user'}) },
        { icon: 'lock', caption: 'User Roles', onSelect: () => this.ngGoto('users-import', {objectType: 'userRoles'}) },
        { icon: 'copy', caption: 'Forms', onSelect: () => this.ngGoto('users-import', {objectType: 'extensions'}) },
        { icon: 'table', caption: 'View Past Imports', onSelect: () => this.ngGoto('users-import-jobs') }
      ]
    },

    exportOpts: function() {
      return [
        { icon: 'user', caption: 'Users', onSelect: () => this.exportRecords('user') },
        { icon: 'lock', caption: 'User Roles', onSelect: () => this.exportRecords('userRoles') },
        { icon: 'copy', caption: 'User Forms', onSelect: () => this.exportForms() }
      ]
    },

    moreOpts: function() {
      let opts = [
        { icon: 'bullhorn', caption: 'New Announcement', onSelect: () => this.$refs.announcementDialog.open() }
      ];

      if (this.$ui.global.appProps.plugins.indexOf('os-extras') && authSvc.isAllowed('institute-admin')) {
        //
        // temporary. will go away when first class support for plugin views is implemented
        //
        opts.push({ icon: 'download', caption: 'Export Login Activity', onSelect: () => this.ngGoto('export-login-audit') });
        opts.push({ icon: 'tachometer-alt', caption: 'Active Users', onSelect: () => this.ngGoto('active-users-report') });
      }

      return opts;
    }
  }
}
</script>
