<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span v-if="!ctx.bulkUpdate">
        <span v-if="!!ctx.user">
          <h3 v-if="!ctx.user.id">Create User</h3>
          <h3 v-else>{{ctx.user.firstName}} {{ctx.user.lastName}}</h3>
        </span>
      </span>
      <span v-else>
        <h3>Bulk Update Users</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="!ctx.bulkUpdate && ctx.user">
        <os-form ref="userForm" :schema="ctx.addEditFs" :data="ctx" @input="handleUserChange($event)">
          <div>
            <os-button primary label="Create" v-if="!ctx.user.id" @click="saveOrUpdate" />
            <os-button primary label="Update" v-else @click="saveOrUpdate" />
            <os-button label="Cancel" @click="cancel" />
          </div>
        </os-form>
      </div>
      <div v-if="ctx.bulkUpdate">
        <os-form ref="userForm" :schema="ctx.bulkEditFs" :data="ctx" @input="handleUserChange($event)">
          <div>
            <os-button primary label="Update" @click="bulkUpdate" />
            <os-button label="Cancel" @click="cancel" />
          </div>
        </os-form>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import userSchema from '@/administrative/schemas/users/user.js';
import addEditSchema from '@/administrative/schemas/users/addedit-schema.js';
import bulkEditSchema from '@/administrative/schemas/users/bulkedit-schema.js';
import editProfileSchema from '@/administrative/schemas/users/edit-profile-schema.js';

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import itemsSvc from '@/common/services/ItemsHolder.js';
import userSvc from '@/administrative/services/User.js';
import formUtil from '@/common/services/FormUtil.js';

export default {
  name: 'UserAddEdit',

  props: ['userId', 'editProfile'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    const ctx = reactive({
      user: null,

      bcrumb: [
        {url: routerSvc.getUrl('UsersList'), label: 'Users'}
      ],

      addEditFs: {rows: []},

      bulkEditFs: {rows: []},

      bulkUpdate: false,

      currentUser: ui.currentUser
    });

    if (props.userId && +props.userId > 0) {
      if (!props.editProfile || props.editProfile != true) {
        ctx.addEditFs = formUtil.getFormSchema(userSchema.fields, addEditSchema.layout);
      } else {
        ctx.addEditFs = formUtil.getFormSchema(userSchema.fields, editProfileSchema.layout);
      }
    } else {
      ctx.user  = { dnd: false, type: 'NONE', apiUser: false };
      let users = itemsSvc.getItems('users');
      itemsSvc.clearItems('users');

      if (users && users.length > 0) {
        ctx.user       = {};
        ctx.users      = users;
        ctx.bulkUpdate = true;
        ctx.bulkEditFs = formUtil.getFormSchema(userSchema.fields, bulkEditSchema.layout);
      } else {
        ctx.addEditFs = formUtil.getFormSchema(userSchema.fields, addEditSchema.layout);
      }
    }

    return { ctx };
  },

  created() {
    if (this.userId && +this.userId > 0) {
      userSvc.getUserById(+this.userId).then(
        user => {
          this.ctx.user = user;
          this.selectedInstitute = user.instituteName;
        }
      );
    }
  },

  methods: {
    handleUserChange: function({field, value, data}) {
      Object.assign(this.ctx.user, data.user);
      if (field.name == 'instituteName' && this.selectedInstitute != value) {
        this.selectedInstitute = value;
        this.ctx.user.primarySite = undefined;
      }
    },

    saveOrUpdate: function() {
      if(!this.$refs.userForm.validate()) {
        return;
      }

      let self = this;
      userSvc.saveOrUpdate(this.ctx.user).then(
        function(result) {
          if (self.editProfile) {
            routerSvc.back();
          } else {
            routerSvc.goto('UserOverview', {userId: result.id});
          }
        }
      );
    },

    bulkUpdate: function() {
      let detail = {};
      let self = this;
      Object.keys(this.ctx.user).forEach(
        function(prop) {
          if (Object.prototype.hasOwnProperty.call(self.ctx.user, prop)) {
            if (self.ctx.user[prop]) {
              detail[prop] = self.ctx.user[prop];
            }
          }
        }
      );

      userSvc.bulkUpdate({ids: this.ctx.users.map(u => u.id), detail: detail}).then(
        function() {
          let count = self.ctx.users.length;
          alertsSvc.info('Updated ' + count + (count > 1 ? ' users' : ' user'));
          routerSvc.back();
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
