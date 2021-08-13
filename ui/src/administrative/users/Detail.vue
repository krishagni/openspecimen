<template>
  <Page>
    <PageHeader>
      <template #breadcrumb>
        <Breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>{{ctx.user.firstName}} {{ctx.user.lastName}}</h3>
      </span>
    </PageHeader>
    <PageBody>
      <Overview :schema="userSchema" :object="ctx.user"></Overview>
    </PageBody>
  </Page>
</template>

<script>
import { reactive, inject } from 'vue';

import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody from '@/common/components/PageBody.vue';

import Overview from '@/common/components/Overview.vue';

import userSvc from '@/administrative/services/User.js';

import userSchema from '@/administrative/users/user-schema.json';

export default {
  name: 'UserDetail',

  props: ['userId'],

  inject: ['ui'],

  components: {
    Page,
    PageHeader,
    Breadcrumb,
    PageBody,
    Overview
  },

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      user: {},

      bcrumb: [
        {url: ui.ngServer + '#/users', label: 'Users', target: '_parent'}
      ]
    });

    
    userSvc.getUserById(+props.userId).then(user => ctx.user = user);
    return { ctx, userSchema };
  },

  methods: {
  }
}
</script>
