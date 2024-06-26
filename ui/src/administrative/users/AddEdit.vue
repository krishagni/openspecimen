<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span v-if="!ctx.bulkUpdate">
        <span v-if="!!ctx.user">
          <h3 v-if="!ctx.user.id">
            <span v-t="'users.create'">Create User</span>
          </h3>
          <h3 v-else>
            <span>{{$filters.username(ctx.user)}}</span>
          </h3>
        </span>
      </span>
      <span v-else>
        <h3 v-t="'users.bulk_update'">Bulk Update Users</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="!ctx.bulkUpdate && ctx.user">
        <os-form ref="userForm" :schema="ctx.addEditFs" :data="ctx" @input="handleUserChange($event)">
          <div>
            <os-button primary :label="$t('common.buttons.create')" v-if="!ctx.user.id" @click="saveOrUpdate" />
            <os-button primary :label="$t('common.buttons.update')" v-else @click="saveOrUpdate" />
            <os-button text    :label="$t('common.buttons.cancel')" @click="cancel" />
          </div>
        </os-form>
      </div>
      <div v-if="ctx.bulkUpdate">
        <os-form ref="userForm" :schema="ctx.bulkEditFs" :data="ctx" @input="handleUserChange($event)">
          <div>
            <os-button primary :label="$t('common.buttons.update')" @click="bulkUpdate" />
            <os-button text    :label="$t('common.buttons.cancel')" @click="cancel" />
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
import itemsSvc  from '@/common/services/ItemsHolder.js';
import i18n      from '@/common/services/I18n.js';
import userSvc   from '@/administrative/services/User.js';
import formUtil  from '@/common/services/FormUtil.js';

export default {
  name: 'UserAddEdit',

  props: ['userId', 'editProfile'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    const ctx = reactive({
      hasWf: (ui.global.appProps.plugins || []).indexOf('task-manager') > -1,

      user: null,

      bcrumb: [
        {url: routerSvc.getUrl('UsersList', {userId: -1}), label: i18n.msg('users.list')}
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
      const users = itemsSvc.getItems('users');
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
      if (field.name == 'user.instituteName' && this.selectedInstitute != value) {
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
          if (self.editProfile || self.ctx.user.id > 0) {
            routerSvc.back();
          } else {
            routerSvc.goto('UserDetail.Overview', {userId: result.id});
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
            if (self.ctx.user[prop] != null && self.ctx.user[prop] != undefined) {
              detail[prop] = self.ctx.user[prop];
            }
          }
        }
      );

      userSvc.bulkUpdate({ids: this.ctx.users.map(u => u.id), detail: detail}).then(
        function() {
          alertsSvc.success({code: 'users.updated', args: {count: self.ctx.users.length}});
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
