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
            <router-link :to="{name: 'UserOverview'}">
              <Icon name="eye" />
            </router-link>
          </li>
          <li>
            <router-link :to="{name: 'UserRoles'}">
              <Icon name="users" />
            </router-link>
          </li>
          <li>
            <router-link :to="{name: 'UserFormsList'}">
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
import { reactive } from 'vue';

import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import Breadcrumb from '@/common/components/Breadcrumb.vue';
import PageBody from '@/common/components/PageBody.vue';
import SideMenu from '@/common/components/SideMenu.vue';
import Icon from '@/common/components/Icon.vue';

import routerSvc from '@/common/services/Router.js';
import userSvc from '@/administrative/services/User.js';
import userResources from '@/administrative/users/Resources.js';

export default {
  name: 'UserDetail',

  props: ['userId'],

  components: {
    Page,
    PageHeader,
    Breadcrumb,
    PageBody,
    SideMenu,
    Icon
  },

  setup(props) {
    let ctx = reactive({
      user: {},

      bcrumb: [
        {url: routerSvc.getUrl('UsersList'), label: 'Users'}
      ]
    });

    
    userSvc.getUserById(+props.userId).then(user => ctx.user = user);
    return { ctx, userResources };
  },

  methods: {
  }
}
</script>
