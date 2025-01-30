<template>
  <os-page-toolbar>
    <template #default>
      <div v-if="(ui.currentUser.admin || !ctx.user.admin) && ctx.user.activityStatus != 'Pending'">
        <os-button left-icon="edit"
          :label="$t('common.buttons.edit')" @click="goto('UserAddEdit', {userId: ctx.user.id})"
          v-if="updateAllowed"
        />

        <os-button left-icon="edit"
          :label="$t('common.buttons.edit')" @click="goto('UserEditProfile', {userId: ctx.user.id})"
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

        <os-button left-icon="key"
          :label="$t('users.change_password')" @click="goto('UserChangePassword', {userId: ctx.user.id})"
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
      <os-overview :schema="userSchema.fields" :object="ctx" :col-type="'md'"/>
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

      routeQuery: route.query
    });

    
    return { ctx, userSchema, userResources };
  },

  created() {
    this.setupUser();
  },

  watch: {
    user: function() {
      this.setupUser();
    }
  },

  computed: {
    updateAllowed: function() {
      return userResources.isUpdateAllowed();
    }
  },

  methods: {
    setupUser: function() {
      const ctx = this.ctx;
      ctx.user = this.user;
      ctx.deleteOpts = {
        type: this.$t('users.singular'),
        title: this.user.firstName + ' ' + this.user.lastName,
        dependents: () => userSvc.getDependents(this.user),
        deleteObj: () => userSvc.delete(this.user)
      };

      ctx.userObjs = [{objectName: 'user', objectId: this.user.id}];
    },

    goto: (name, params, query) => routerSvc.goto(name, params, query),

    updateStatus: function(status, msg) {
      let self = this;
      userSvc.updateStatus(self.ctx.user, status).then(
        (savedUser) => {
          self.ctx.user = savedUser;
          Object.assign(this.user, savedUser); // OPSMN-5800: switch between tabs
          alertSvc.success({code: msg, args: {count: 1}});
        }
      );
    },

    lock: function() {
      this.updateStatus('Locked', 'users.locked');
    },

    activate: function() {
      const status = this.ctx.user.activityStatus;
      const msg = status == 'Locked' ? 'users.unlocked' : (status == 'Closed' ? 'users.reactivated' : 'users.approved');
      this.updateStatus('Active', msg);
    },

    archive: function() {
      this.updateStatus('Closed', 'users.archived');
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
            userSvc.impersonate(this.ctx.user).then(() => routerSvc.ngGoto(''));
          }
        }
      );
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    }
  }
}
</script>
