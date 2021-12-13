<template>
  <os-page-toolbar>
    <template #default>
      <div v-if="(ui.currentUser.admin || !ctx.user.admin) && ctx.user.activityStatus != 'Pending'">
        <os-button left-icon="edit"
          label="Edit" @click="goto('UserAddEdit', {userId: ctx.user.id})"
          v-if="updateAllowed"
        />

        <os-button left-icon="edit"
          label="Edit" @click="goto('UserEditProfile', {userId: ctx.user.id})"
          v-if="!updateAllowed && ui.currentUser.id == ctx.user.id"
        />

        <os-button left-icon="lock"
          label="Lock" @click="lock"
          v-if="updateAllowed &&
            ctx.user.activityStatus != 'Locked' &&
            ctx.user.activityStatus != 'Closed' &&
            ctx.user.type != 'CONTACT'"
        />

        <os-button left-icon="lock-open"
          label="Unlock" @click="activate"
          v-if="updateAllowed && ctx.user.activityStatus == 'Locked'"
        />

        <os-button left-icon="archive"
          label="Archive" @click="archive"
          v-if="updateAllowed && ctx.user.activityStatus != 'Closed'"
        />

        <os-button left-icon="check"
          label="Reactivate" @click="activate"
          v-if="updateAllowed && ctx.user.activityStatus == 'Closed'"
        />

        <os-button left-icon="trash"
          label="Delete" @click="deleteUser"
          v-show-if-allowed="userResources.deleteOpts"
        />

        <os-button left-icon="key"
          label="Reset Password" @click="goto('UserChangePassword', {userId: ctx.user.id})"
          v-if="ctx.user.type != 'CONTACT' && ctx.user.domainName == 'openspecimen' &&
            (ui.currentUser.id == ctx.user.id || ui.currentUser.admin || ui.currentUser.instituteAdmin) &&
            (ctx.user.activityStatus == 'Active' || ctx.user.activityStatus == 'Expired')"
        />

        <os-button left-icon="user-secret"
          label="Impersonate" @click="impersonate"
          v-if="ctx.user.type != 'CONTACT' && ctx.user.activityStatus == 'Active' &&
            ui.currentUser.id != ctx.user.id && ui.currentUser.admin"
        />
      </div>
      <div v-else-if="ctx.user.activityStatus == 'Pending' && ui.currentUser.admin">
        <os-button left-icon="check"
          label="Approve User" @click="activate"
        />
        <os-button left-icon="times"
          label="Reject User" @click="deleteUser"
        />
      </div>

    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <div v-if="ctx.user.activityStatus == 'Locked'">
        <os-message type="info">
          <span>User account has been locked.</span>
        </os-message>
      </div>

      <os-overview :schema="userSchema.fields" :object="ctx" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.userObjs" v-if="ctx.user.id" />
    </os-grid-column>
  </os-grid>

  <os-confirm ref="confirmImpersonate">
    <template #title>
      <span>Sign-in as {{ctx.user.firstName}} {{ctx.user.lastName}}...</span>
    </template>

    <template #message>
      <span>An email will be sent to <b>{{ctx.user.firstName}} {{ctx.user.lastName}}</b> to let them know you've signed-in to their account. The email will include details like your name, email address, and device IP address. Do you want to proceed?</span>
    </template>
  </os-confirm>

  <os-delete-object ref="deleteObj" :input="ctx.deleteOpts" />
</template>

<script>
import { reactive, watchEffect } from 'vue';

import alertSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import userSvc from '@/administrative/services/User.js';

import userResources from '@/administrative/users/Resources.js';
import userSchema from '@/administrative/schemas/users/user.js';


export default {
  name: 'UserDetail',

  props: ['user'],

  inject: ['ui'],

  setup(props) {
    let ctx = reactive({
      user: {},          // details of user being displayed

      deleteOpts: {},    // delete dialog details

      userObjs: []       // list of objects whose audit info is being queried
    });

    
    watchEffect(
      () => {
        ctx.user = props.user;
        ctx.deleteOpts = {
          type: 'User',
          title: props.user.firstName + ' ' + props.user.lastName,
          dependents: () => userSvc.getDependents(props.user),
          deleteObj: () => userSvc.delete(props.user)
        };
        ctx.userObjs = [{objectName: 'user', objectId: props.user.id}];
      }
    );

    return { ctx, userSchema, userResources };
  },

  computed: {
    updateAllowed: function() {
      return userResources.isUpdateAllowed();
    }
  },

  methods: {
    goto: (name, params, query) => routerSvc.goto(name, params, query),

    updateStatus: function(status, msg) {
      let self = this;
      userSvc.updateStatus(self.ctx.user, status).then(
        (savedUser) => {
          self.ctx.user = savedUser;
          alertSvc.success(msg);
        }
      );
    },

    lock: function() {
      this.updateStatus('Locked', 'User locked!');
    },

    activate: function() {
      let status = this.ctx.user.activityStatus;
      let message = status == 'Locked' ?
        'User unlocked!' :
        (status == 'Closed' ? 'User reactivated!' : 'User request approved!');
      this.updateStatus('Active', message);
    },

    archive: function() {
      this.updateStatus('Closed', 'User archived!');
    },

    deleteUser: function() {
      this.$refs.deleteObj.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('UsersList');
          }
        }
      );
    },

    impersonate: function() {
      this.$refs.confirmImpersonate.open().then(
        () => userSvc.impersonate(this.ctx.user).then(() => routerSvc.ngGoto(''))
      );
    }
  }
}
</script>
