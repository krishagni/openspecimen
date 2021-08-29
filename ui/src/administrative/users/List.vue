<template>
  <Page>
    <PageHeader>
      <template #breadcrumb v-if="ctx.group">
        <Breadcrumb :items="ctx.ugCrumb" />
      </template>

      <span>
        <h3 v-if="!ctx.group">Users</h3>
        <h3 v-else>{{ctx.group.name}}</h3>
      </span>

      <template #right>
        <ListSize
          :list="ctx.users"
          :page-size="ctx.pageSize"
          :list-size="ctx.usersCount"
          @updateListSize="getUsersCount"
        />
      </template>
    </PageHeader>
    <PageBody v-if="ctx.inited">
      <PageToolbar>
        <template #default>
          <span v-if="ctx.selectedUsers.length == 0 && !ctx.group">
            <Button left-icon="plus" label="Create" @click="goto('UserAddEdit', {userId: -1})" />

            <Button left-icon="users" label="User Groups" @click="ngGoto('user-groups')" />

            <Menu label="Import" :options="importOpts" />

            <Menu label="Export" :options="exportOpts" v-show-if-allowed="userResources.importOpts"/>

            <Menu label="More" :options="moreOpts" />

            <Button left-icon="question-circle" label="Help" @click="help" />
          </span>

          <span v-if="ctx.selectedUsers.length > 0">
            <Button left-icon="edit" label="Edit" @click="bulkEdit" />

            <AssignGroup v-if="!ctx.group" @addToGroup="addToGroup" />

            <Button v-if="ctx.group" left-icon="times" label="Remove from Group" @click="removeFromGroup"/>

            <Button left-icon="archive" label="Archive" @click="archiveUsers" />

            <Button left-icon="check" label="Reactivate" @click="reactivateUsers" />

            <Button left-icon="trash" label="Delete" @click="deleteUsers" />

            <Button left-icon="lock" label="Lock" @click="lockUsers" />

            <Button left-icon="unlock" label="Unlock" @click="unlockUsers" />

            <Button left-icon="thumbs-up" label="Approve" @click="approveUsers" />

            <Menu label="Export" :options="exportOpts" v-show-if-allowed="userResources.importOpts"/>
          </span>
        </template>

        <template #right>
          <Button left-icon="search" label="Search" @click="openSearch" />
        </template>
      </PageToolbar>

      <ListView
        :data="ctx.users"
        :columns="ctx.columns"
        :filters="ctx.filters"
        :query="ctx.query"
        :allowSelection="true"
        :loading="ctx.loading"
        @filtersUpdated="loadUsers"
        @selectedRows="onUsersSelection"
        @rowClicked="onUserRowClick"
        ref="listView"
      >
      </ListView>

      <ConfirmDelete ref="deleteDialog">
        <template #message>
          <span>Are you sure you want to delete the selected users?</span>
        </template>
      </ConfirmDelete>

      <Announcement ref="announcementDialog" />
    </PageBody>
  </Page>
</template>

<script>
import { reactive, inject } from 'vue';
import { format } from 'date-fns';

import ListSize from '@/common/components/ListSize.vue';
import ListView from '@/common/components/ListView.vue';
import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody from '@/common/components/PageBody.vue';
import PageToolbar from '@/common/components/PageToolbar.vue';
import Button from '@/common/components/Button.vue';
import Menu from '@/common/components/Menu.vue';
import ConfirmDelete from '@/common/components/ConfirmDelete.vue';

import instituteSvc from '@/administrative/services/Institute.js';
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

  inject: ['ui'],

  props: ['filters', 'groupId'],

  components: {
    Page,
    PageHeader,
    Breadcrumb,
    PageBody,
    PageToolbar,
    Button,
    ListSize,
    ListView,
    Menu,
    ConfirmDelete,
    AssignGroup,
    Announcement
  },

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      inited: false,

      group: undefined,

      users: [],

      selectedUsers: [],

      columns: [
        {
          name: 'name',
          caption: 'Name',
          value: function (user) {
            return user.firstName + ' ' + user.lastName;
          },
          href: function (user) {
            return '../#/users/' + user.rowObject.id + '/overview';
          },
          hrefTarget: '_parent'
        },
        { name: 'emailAddress', caption: 'Email Address' },
        { name: 'loginName', caption: 'Login Name' },
        { name: 'instituteName', caption: 'Institute' },
        { name: 'primarySite', caption: 'Primary Site' },
        {
          name: 'activeSince',
          caption: 'Active Since',
          value: function (user) {
            if (user.creationDate) {
              return format(new Date(user.creationDate), ui.os.global.dateFmt);
            }
            return undefined;
          }
        }
      ],

      filters: [
        { name: 'name', type: 'text', caption: 'Name' },
        { name: 'loginName', type: 'text', caption: 'Login Name' },
        { name: 'institute', type: 'dropdown', caption: 'Institute',
          listSource: {
            displayProp: 'name',
            selectProp: 'name',
            loadFn: (opts) => {
              opts = opts || {};
              opts.name = opts.query;
              return instituteSvc.getInstitutes(opts);
            }
          }
        },
        { name: 'group', type: 'dropdown', caption: 'User Group',
          listSource: {
            displayProp: 'name',
            selectProp: 'name',
            loadFn: (opts) => userGrpSvc.getUserGroups(opts)
          }
        },
        { name: 'activityStatus', type: 'dropdown', caption: 'Activity Status',
          listSource: {
            options: ['Active', 'Archived', 'Expired', 'Locked', 'Pending']
          }
        },
        { name: 'type', type: 'dropdown', caption: 'Type',
          listSource: {
            selectProp: 'name',
            displayProp: 'caption',
            options: [
              { name: 'SUPER', caption: 'Super Admin' },
              { name: 'INSTITUTE', caption: 'Institute Admin' },
              { name: 'CONTACT', caption: 'Contact' },
              { name: 'NONE', caption: 'Regular' }
            ]
          }
        }
      ],

      query: props.filters,

      pageSize: undefined,

      usersCount: -1,

      ugCrumb: [ {url: ui.ngServer + '#/user-groups', label: 'User Groups', target: '_parent'} ]
    });

    if (props.groupId) {
      //
      // remove institute and group filters
      //
      ctx.filters.splice(2, 2);

      userGrpSvc.getUserGroup(props.groupId)
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

  methods: {
    openSearch: function () {
      this.$refs.listView.toggleShowFilters();
    },

    loadUsers: function ({filters, uriEncoding, pageSize}) {
      this.ctx.loading = true;
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;

      let params = {filters: uriEncoding};
      if (this.ctx.group) {
        params.groupId = this.ctx.group.id;
      }
      routerSvc.ngGoto(undefined, params, {notify: false});

      let opts = Object.assign({maxResults: pageSize}, filters);
      if (this.ctx.group) {
        opts.group = this.ctx.group.name;
      }

      if (!this.ui.currentUser.admin) {
        opts.institute = this.ui.currentUser.instituteName;
      }

      userSvc.getUsers(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.users = resp;
      });
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

    onUserRowClick: function(user) {
      routerSvc.ngGoto('user-detail.overview', {userId: user.id});
    },

    bulkEdit: function() {
      let users = this.ctx.selectedUsers.map(user => ({id: user.rowObject.id}));
      itemsSvc.ngSetItems('users', users);
      routerSvc.ngGoto('user-bulk-edit');
    },

    updateStatus: function(fromStatuses, toStatus, msg) {
      let users = this.ctx.selectedUsers
        .map(user => user.rowObject)
        .filter(user => !fromStatuses || fromStatuses.length == 0 || fromStatuses.indexOf(user.activityStatus) != -1);

      let usersMap = {};
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
      let users = this.ctx.selectedUsers.map(user => user.rowObject);
      if (users.length == 0) {
        return;
      }

      let instituteId = users[0].instituteId;
      for (let user of users) {
        if (user.instituteId != instituteId) {
          alertSvc.error('Users of multiple institutes cannot be added to the group.');
          return;
        }
      }

      if (group) {
        userGroupSvc.addUsers(group, users).then(() => alertSvc.success('Users added to the group ' + group.name));
      } else {
        itemsSvc.ngSetItems(
          'users',
          users.map(user => ({id: user.id, insituteId: user.instituteId, instituteName: user.instituteName}))
        );
        routerSvc.ngGoto('user-group-addedit', {groupId: ''});
      }
    },

    removeFromGroup: function() {
      let users = this.ctx.selectedUsers.map(user => user.rowObject);
      if (!users || users.length == 0) {
        return;
      }

      let self = this;
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
      let users = this.ctx.selectedUsers.map(user => user.rowObject);

      if (!this.ui.currentUser.admin) {
        let admins = users.filter(user => user.admin == true)
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
      let userIds = this.ctx.selectedUsers.map(user => user.rowObject.id);
      exportSvc.exportRecords({objectType: type, recordIds: userIds});
    },

    exportForms: function() {
      let users = this.ctx.selectedUsers.map(user => ({emailAddress: user.rowObject.emailAddress}));
      itemsSvc.ngSetItems('users', users);
      routerSvc.ngGoto('user-export-forms');
    },

    ngGoto: routerSvc.ngGoto,

    goto: routerSvc.goto,

    help: function() {
      window.open('http://help.openspecimen.org/user', '_blank').focus();
    }
  },

  computed: {
    importOpts: function() {
      return [
        { icon: 'user', caption: 'Users', onSelect: () => this.ngGoto('user-import', {objectType: 'user'}) },
        { icon: 'lock', caption: 'User Roles', onSelect: () => this.ngGoto('user-import', {objectType: 'userRoles'}) },
        { icon: 'copy', caption: 'Forms', onSelect: () => this.ngGoto('user-import', {objectType: 'extensions'}) },
        { icon: 'table', caption: 'View Past Imports', onSelect: () => this.ngGoto('user-import-jobs') }
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

      if (this.ui.global.appProps.plugins.indexOf('os-extras') && authSvc.isAllowed('institute-admin')) {
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
