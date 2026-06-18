<template>
  <os-page-toolbar>
    <template #default>
      <div v-if="(ui.currentUser.admin || !ctx.user.admin) && ctx.user.activityStatus != 'Pending'">
        <os-button-link left-icon="edit"
          :label="$t('common.buttons.edit')" :url="editUserUrl"
          v-if="updateAllowed"
        />

        <os-button-link left-icon="edit"
          :label="$t('common.buttons.edit')" :url="editProfileUrl"
          v-if="!updateAllowed && ui.currentUser.id == ctx.user.id"
        />

        <os-button left-icon="lock"
          :label="$t('users.lock')" @click="lock"
          v-if="updateAllowed &&
            ctx.user.activityStatus != 'Locked' &&
            ctx.user.activityStatus != 'Closed' &&
            ctx.user.type != 'CONTACT'"
        />

        <os-button left-icon="lock-open"
          :label="$t('users.unlock')" @click="activate"
          v-if="updateAllowed && ctx.user.activityStatus == 'Locked'"
        />

        <os-button left-icon="archive"
          :label="$t('common.buttons.archive')" @click="archive"
          v-if="updateAllowed && ctx.user.activityStatus != 'Closed'"
        />

        <os-button left-icon="check"
          :label="$t('common.buttons.reactivate')" @click="activate"
          v-if="updateAllowed && ctx.user.activityStatus == 'Closed'"
        />

        <os-button left-icon="trash"
          :label="$t('common.buttons.delete')" @click="deleteUser"
          v-show-if-allowed="userResources.deleteOpts"
        />

        <os-button-link left-icon="key"
          :label="$t('users.change_password')" :url="changePasswordUrl"
          v-if="ctx.user.type != 'CONTACT' && ctx.user.domainName == 'openspecimen' &&
            (ui.currentUser.id == ctx.user.id || ui.currentUser.admin || ui.currentUser.instituteAdmin) &&
            (ctx.user.activityStatus == 'Active' || ctx.user.activityStatus == 'Expired')"
        />

        <os-button left-icon="user-secret"
          :label="$t('users.impersonate')" @click="impersonate"
          v-if="ctx.user.type != 'CONTACT' && ctx.user.activityStatus == 'Active' &&
            ui.currentUser.id != ctx.user.id && ui.currentUser.admin"
        />

        <os-button left-icon="history" :label="$t('audit.trail')" @click="viewAuditTrail" />
      </div>
      <div v-else-if="ctx.user.activityStatus == 'Pending' && ui.currentUser.admin">
        <os-button left-icon="check"
          :label="$t('users.approve')" @click="activate"
        />
        <os-button left-icon="times"
          :label="$t('users.reject')" @click="deleteUser"
        />

        <os-button left-icon="history" :label="$t('audit.trail')" @click="viewAuditTrail" />
      </div>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="userSchema.fields" :object="ctx" />

      <os-section>
        <template #title>
          <span v-t="'user_groups.list'">User Groups</span>
        </template>

        <template #content>
          <table class="os-table muted-header os-border">
            <thead>
              <tr>
                <th>
                  <span v-t="'user_groups.name'">Name</span>
                </th>
                <th>
                  <span v-t="'user_groups.description'">Description</span>
                </th>
              </tr>
            </thead>
            <tbody class="os-table-body">
              <tr v-if="ctx.loadingUserGroups">
                <td colspan="2">
                  <span v-t="'common.loading'">Loading...</span>
                </td>
              </tr>
              <tr v-for="group in ctx.userGroups" :key="group.id">
                <td>{{group.name}}</td>
                <td>{{group.description}}</td>
              </tr>
              <tr v-if="!ctx.loadingUserGroups && ctx.userGroups.length == 0">
                <td colspan="2">
                  <span v-t="'user_groups.no_groups'">No user groups to show.</span>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="user-groups-actions" v-if="ctx.hasMoreUserGroups">
            <span v-t="'user_groups.more_groups'">There are more user groups. Click "View All" to see all groups.</span>
            <os-button text :label="$t('common.buttons.view_all')" @click="viewAllUserGroups" />
          </div>
        </template>
      </os-section>
    </os-grid-column>
  </os-grid>

  <os-confirm ref="confirmImpersonate">
    <template #title>
      <span v-t="{path: 'users.impersonate_as', args: ctx.user}">Sign-in as...</span>
    </template>

    <template #message>
      <span v-t="{path: 'users.impersonate_tnc', args: ctx.user}">An email will be sent to user to let them know you've signed-in to their account. The email will include details like your name, email address, and device IP address. Do you want to proceed?</span>
    </template>
  </os-confirm>

  <os-delete-object ref="deleteObj" :input="ctx.deleteOpts" />

  <os-audit-trail ref="auditTrailDialog" :objects="ctx.userObjs" />
</template>

<script>
import { inject, reactive } from 'vue';
import { useRoute } from 'vue-router';

import alertSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import util from '@/common/services/Util.js';
import userGrpSvc from '@/administrative/services/UserGroup.js';
import userSvc from '@/administrative/services/User.js';

import userResources from '@/administrative/users/Resources.js';
import userSchema from '@/administrative/schemas/users/user.js';


export default {
  name: 'UserDetail',

  props: ['user'],

  inject: ['ui'],

  setup() {
    const route = useRoute();
    const ui = inject('ui');

    const ctx = reactive({
      hasWf: (ui.global.appProps.plugins || []).indexOf('task-manager') > -1,

      user: {},          // details of user being displayed

      deleteOpts: {},    // delete dialog details

      userObjs: [],      // list of objects whose audit info is being queried

      userGroups: [],

      hasMoreUserGroups: false,

      loadingUserGroups: false,

      routeQuery: route.query
    });

    
    return { ctx, userSchema, userResources };
  },

  created() {
    this._setupUser();
  },

  watch: {
    user: function() {
      this._setupUser();
    }
  },

  computed: {
    updateAllowed: function() {
      return userResources.isUpdateAllowed();
    },

    editUserUrl: function() {
      return routerSvc.getUrl('UserAddEdit', {userId: this.ctx.user.id});
    },

    editProfileUrl: function() {
      return routerSvc.getUrl('UserEditProfile', {userId: this.ctx.user.id});
    },

    changePasswordUrl: function() {
      return routerSvc.getUrl('UserChangePassword', {userId: this.ctx.user.id});
    }
  },

  methods: {
    lock: function() {
      this._updateStatus('Locked', 'users.locked');
    },

    activate: function() {
      const status = this.ctx.user.activityStatus;
      const msg = status == 'Locked' ? 'users.unlocked' : (status == 'Closed' ? 'users.reactivated' : 'users.approved');
      this._updateStatus('Active', msg);
    },

    archive: function() {
      this._updateStatus('Closed', 'users.archived');
    },

    deleteUser: function() {
      this.$refs.deleteObj.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('UsersList', {userId: -2}, this.ctx.routeQuery);
          }
        }
      );
    },

    impersonate: function() {
      this.$refs.confirmImpersonate.open().then(
        (resp) => {
          if (resp == 'proceed') {
            userSvc.impersonate(this.ctx.user).then(() => routerSvc.reloadView('HomePage'));
          }
        }
      );
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    },

    viewAllUserGroups: function() {
      const filters = util.uriEncode({userId: this.ctx.user.id});
      routerSvc.goto('UserGroupsList', {}, {filters});
    },

    _setupUser: function() {
      const ctx = this.ctx;
      ctx.user = this.user;
      ctx.deleteOpts = {
        type: this.$t('users.singular'),
        title: this.user.firstName + ' ' + this.user.lastName,
        dependents: () => userSvc.getDependents(this.user),
        deleteObj: () => userSvc.delete(this.user)
      };

      ctx.userObjs = [{objectName: 'user', objectId: this.user.id}];
      this._loadUserGroups();
    },

    _updateStatus: function(status, msg) {
      let self = this;
      userSvc.updateStatus(self.ctx.user, status).then(
        (savedUser) => {
          self.ctx.user = savedUser;
          Object.assign(this.user, savedUser); // OPSMN-5800: switch between tabs
          alertSvc.success({code: msg, args: {count: 1}});
        }
      );
    },

    _loadUserGroups: async function() {
      const ctx = this.ctx;
      ctx.userGroups = [];
      ctx.hasMoreUserGroups = false;
      ctx.loadingUserGroups = false;

      if (!ctx.user || !ctx.user.id) {
        return;
      }

      const userId = ctx.user.id;
      ctx.loadingUserGroups = true;
      try {
        const groups = await userGrpSvc.getGroups({userId, maxResults: 11});
        if (ctx.user.id == userId) {
          ctx.userGroups = groups.slice(0, 10);
          ctx.hasMoreUserGroups = groups.length > 10;
        }
      } finally {
        if (ctx.user.id == userId) {
          ctx.loadingUserGroups = false;
        }
      }
    }
  }
}
</script>

<style scoped>
.user-groups-actions {
  align-items: center;
  display: flex;
  gap: 0.75rem;
  justify-content: flex-start;
  margin-top: 0.75rem;
}
</style>
