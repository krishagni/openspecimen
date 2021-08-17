<template>
  <PageToolbar>
    <template #default>
      <div v-if="(ui.currentUser.admin || !ctx.user.admin) && ctx.user.activityStatus != 'Pending'">
        <Button left-icon="edit"
          label="Edit"
          @click="ngGoto('user-addedit', {userId: ctx.user.id})"
          v-if="userResources.updateAllowed"
        />

        <Button left-icon="lock"
          label="Lock"
          @click="lock"
          v-if="userResources.updateAllowed &&
            ctx.user.activityStatus != 'Locked' &&
            ctx.user.activityStatus != 'Closed' &&
            ctx.user.type != 'CONTACT'"
        />

        <Button left-icon="lock-open"
          label="Unlock"
          @click="activate"
          v-if="userResources.updateAllowed && ctx.user.activityStatus == 'Locked'"
        />

        <Button left-icon="archive"
          label="Archive"
          @click="archive"
          v-if="userResources.updateAllowed && ctx.user.activityStatus != 'Closed'"
        />

        <Button left-icon="check"
          label="Reactivate"
          @click="activate"
          v-if="userResources.updateAllowed && ctx.user.activityStatus == 'Closed'"
        />

        <Button left-icon="trash"
          label="Delete"
          @click="deleteUser"
          v-show-if-allowed="userResources.deleteOpts"
        />

        <Button left-icon="key"
          label="Reset Password"
          @click="ngGoto('user-password', {userId: ctx.user.id})"
          v-if="ctx.user.type != 'CONTACT' && ctx.user.domainName == 'openspecimen' &&
            ui.currentUser.id != ctx.user.id &&
            (ui.currentUser.admin || ui.currentUser.instituteAdmin) &&
            (ctx.user.activityStatus == 'Active' || ctx.user.activityStatus == 'Expired')"
        />

        <Button left-icon="user-secret"
          label="Impersonate"
          @click="impersonate"
          v-if="ctx.user.type != 'CONTACT' && ctx.user.activityStatus == 'Active' &&
            ui.currentUser.id != ctx.user.id && ui.currentUser.admin"
        />
      </div>
    </template>
  </PageToolbar>

  <Grid>
    <GridColumn width="8">
      <div v-if="ctx.user.activityStatus == 'Locked'">
        <Message type="info">
          <span>User account has been locked.</span>
        </Message>
      </div>

      <Overview :schema="userSchema" :object="ctx.user"></Overview>
    </GridColumn>

    <GridColumn width="4">
      <AuditOverview :objects="ctx.userObjs" v-if="ctx.user.id"></AuditOverview>
    </GridColumn>
  </Grid>

  <Confirm ref="confirmImpersonate">
    <template #title>
      <span>Sign-in as {{ctx.user.firstName}} {{ctx.user.lastName}}...</span>
    </template>

    <template #message>
      <span>An email will be sent to <b>{{ctx.user.firstName}} {{ctx.user.lastName}}</b> to let them know you've signed-in to their account. The email will include details like your name, email address, and device IP address. Do you want to proceed?</span>
    </template>
  </Confirm>

  <DeleteObject ref="deleteObj" :input="ctx.deleteOpts" />
</template>

<script>
import { reactive, inject, watchEffect } from 'vue';

import PageToolbar from '@/common/components/PageToolbar.vue';
import Overview from '@/common/components/Overview.vue';
import Button from '@/common/components/Button.vue';
import Message from '@/common/components/Message.vue';
import Confirm from '@/common/components/Confirm.vue';
import DeleteObject from '@/common/components/DeleteObject.vue';
import AuditOverview from '@/common/components/AuditOverview.vue';
import Grid from '@/common/components/Grid.vue';
import GridColumn from '@/common/components/GridColumn.vue';

import alertSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import userSvc from '@/administrative/services/User.js';

import userResources from '@/administrative/users/Resources.js';
import userSchema from '@/administrative/users/user-schema.json';


export default {
  name: 'UserDetail',

  props: ['user'],

  inject: ['ui'],

  components: {
    PageToolbar,
    Overview,
    Button,
    Message,
    Confirm,
    DeleteObject,
    AuditOverview,
    Grid,
    GridColumn
  },

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      user: {},

      bcrumb: [
        {url: ui.ngServer + '#/users', label: 'Users', target: '_parent'}
      ],

      deleteOpts: {},

      userObjs: []
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

  methods: {
    ngGoto: routerSvc.ngGoto,

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
            routerSvc.ngGoto('user-list');
          }
        }
      );
    },

    impersonate: function() {
      let self = this;
      this.$refs.confirmImpersonate.open().then(
        () => {
          userSvc.impersonate(self.ctx.user).then(
            (detail) => {
              window.parent.postMessage({
                op: 'impersonate',
                token: detail.impersonateUserToken,
                requestor: 'vueapp'
              }, '*');
            }
          );
        }
      );
    }
  }
}
</script>
