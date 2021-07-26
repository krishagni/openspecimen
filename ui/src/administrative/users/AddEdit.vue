<template>
  <Page>
    <PageHeader>
      <template #breadcrumb>
        <Breadcrumb :items="ctx.bcrumb" />
      </template>

      <span v-if="!!ctx.user">
        <h3 v-if="!ctx.user.id">Create User</h3>
        <h3 v-else>{{ctx.user.firstName}} {{ctx.user.lastName}}</h3>
      </span>
    </PageHeader>
    <PageBody>
      <div v-if="ctx.user">
        <Form :schema="userSchema" :data="ctx.user" @input="handleUserChange($event)">
          <div>
            <Button label="Create" v-if="!ctx.user.id" @click="saveOrUpdate"/>
            <Button label="Update" v-if="!!ctx.user.id" @click="saveOrUpdate"/>
            <Button label="Cancel" />
          </div>
        </Form>
      </div>
    </PageBody>
  </Page>
</template>

<script>
import { reactive, inject } from 'vue';

import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody from '@/common/components/PageBody.vue';

import Button from '@/common/components/Button.vue';
import Form from '@/common/components/Form.vue';

import userSchema from '@/administrative/users/addedit-schema.json';

import routerSvc from '@/common/services/Router.js';
import userSvc from '@/administrative/services/User.js';

export default {
  name: 'UserAddEdit',

  props: ['userId'],

  inject: ['ui'],

  components: {
    Page,
    PageHeader,
    Breadcrumb,
    PageBody,
    Form,
    Button
  },

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      user: null,
      bcrumb: [
        {url: ui.ngServer + '#/users', label: 'Users', target: '_parent'}
      ]
    });

    if (props.userId && +props.userId > 0) {
      userSvc.getUserById(+props.userId).then(user => ctx.user = user);
    } else {
      ctx.user = { dnd: false, type: 'NONE', apiUser: false };
    }

    return {
      ctx,

      userSchema
    };
  },

  methods: {
    handleUserChange: function(event) {
      Object.assign(this.ctx.user, event.data);
    },

    saveOrUpdate: function() {
      userSvc.saveOrUpdate(this.ctx.user).then(
        function(result) {
          routerSvc.ngGoto('user-detail.overview', {userId: result.id});
        }
      );
    }
  }
}
</script>
