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
      <SideMenu>
        <ul>
          <li>
            <router-link to="overview">
              <Icon name="eye" />
            </router-link>
          </li>
          <li>
            <router-link to="roles">
              <Icon name="users" />
            </router-link>
          </li>
          <li>
            <router-link to="forms">
              <Icon name="copy" />
            </router-link>
          </li>
        </ul>
      </SideMenu>

      <router-view :user="ctx.user" v-if="ctx.user.id"> </router-view>

    </PageBody>
  </Page>
</template>

<script>
import { reactive, inject } from 'vue';

import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody from '@/common/components/PageBody.vue';
import SideMenu from '@/common/components/SideMenu.vue';
import Icon from '@/common/components/Icon.vue';

import userSvc from '@/administrative/services/User.js';
import userResources from '@/administrative/users/Resources.js';

export default {
  name: 'UserDetail',

  props: ['userId'],

  inject: ['ui'],

  components: {
    Page,
    PageHeader,
    Breadcrumb,
    PageBody,
    SideMenu,
    Icon
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
    return { ctx, userResources };
  },

  methods: {
  }
}
</script>
