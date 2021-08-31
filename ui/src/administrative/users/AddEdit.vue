<template>
  <Page>
    <PageHeader>
      <template #breadcrumb>
        <Breadcrumb :items="ctx.bcrumb" />
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
    </PageHeader>
    <PageBody>
      <div v-if="!ctx.bulkUpdate && ctx.user">
        <Form ref="userForm" :schema="ctx.addEditFs" :data="ctx.user" @input="handleUserChange($event)">
          <div>
            <Button label="Create" v-if="!ctx.user.id" @click="saveOrUpdate"/>
            <Button label="Update" v-else @click="saveOrUpdate"/>
            <Button label="Cancel" @click="cancel"/>
          </div>
        </Form>
      </div>
      <div v-if="ctx.bulkUpdate">
        <Form ref="userForm" :schema="ctx.bulkEditFs" :data="ctx.user" @input="handleUserChange($event)">
          <div>
            <Button label="Update" @click="bulkUpdate"/>
            <Button label="Cancel" @click="cancel"/>
          </div>
        </Form>
      </div>
    </PageBody>
  </Page>
</template>

<script>
import { reactive } from 'vue';

import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody from '@/common/components/PageBody.vue';

import Button from '@/common/components/Button.vue';
import Form from '@/common/components/Form.vue';

import userSchema from '@/administrative/users/user-schema.json';
import addEditSchema from '@/administrative/users/addedit-schema.json';
import bulkEditSchema from '@/administrative/users/bulkedit-schema.json';
import editProfileSchema from '@/administrative/users/edit-profile-schema.json';

import alertsSvc from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';
import itemsSvc from '@/common/services/ItemsHolder.js';
import userSvc from '@/administrative/services/User.js';
import formUtil from '@/common/services/FormUtil.js';

export default {
  name: 'UserAddEdit',

  props: ['userId', 'editProfile'],

  components: {
    Page,
    PageHeader,
    Breadcrumb,
    PageBody,
    Form,
    Button
  },

  setup(props) {
    let ctx = reactive({
      user: null,

      bcrumb: [
        {url: routerSvc.getUrl('UsersList'), label: 'Users'}
      ],

      addEditFs: {rows: []},

      bulkEditFs: {rows: []}
    });

    if (props.userId && +props.userId > 0) {
      userSvc.getUserById(+props.userId).then(user => ctx.user = user);
      if (!props.editProfile || props.editProfile != true) {
        ctx.addEditFs = formUtil.getFormSchema(userSchema, addEditSchema);
      } else {
        ctx.addEditFs = formUtil.getFormSchema(userSchema, editProfileSchema);
      }
    } else {
      ctx.user  = { dnd: false, type: 'NONE', apiUser: false };
      let users = itemsSvc.getItems('users');
      itemsSvc.clearItems();

      if (users && users.length > 0) {
        ctx.user       = {};
        ctx.users      = users;
        ctx.bulkUpdate = true;
        ctx.bulkEditFs = formUtil.getFormSchema(userSchema, bulkEditSchema);
      } else {
        ctx.addEditFs = formUtil.getFormSchema(userSchema, addEditSchema);
      }
    }

    return { ctx };
  },

  methods: {
    handleUserChange: function(event) {
      Object.assign(this.ctx.user, event.data);
    },

    saveOrUpdate: function() {
      if(!this.$refs.userForm.validate()) {
        return;
      }

      let self = this;
      userSvc.saveOrUpdate(this.ctx.user).then(
        function(result) {
          if (self.editProfile) {
            routerSvc.ngGoto('home', {});
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
