<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>{{ctx.user.firstName}} {{ctx.user.lastName}}</h3>
      </span>
    </os-page-head>
    <os-page-body>
      <os-side-menu>
        <ul>
          <li>
            <router-link :to="{name: 'UserOverview'}">
              <os-icon name="eye" />
            </router-link>
          </li>
          <li v-if="ctx.isUpdateAllowed && ctx.user.type == 'NONE'">
            <router-link :to="{name: 'UserRoles'}">
              <os-icon name="users" />
            </router-link>
          </li>
          <li v-if="ctx.isUpdateAllowed">
            <router-link :to="{name: 'UserFormsList'}">
              <os-icon name="copy" />
            </router-link>
          </li>
          <li v-if="ctx.pfuAllowed">
            <router-link :to="{name: 'UserProfileFormsList'}">
              <os-icon name="user" />
            </router-link>
          </li>

          <os-plugin-views page="user-detail" view="side-menu" />
        </ul>
      </os-side-menu>

      <router-view :user="ctx.user" v-if="ctx.user.id"> </router-view>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, watchEffect } from 'vue';

import routerSvc from '@/common/services/Router.js';
import userSvc from '@/administrative/services/User.js';
import userResources from '@/administrative/users/Resources.js';

export default {
  name: 'UserDetail',

  props: ['userId'],

  setup(props) {
    let ctx = reactive({
      user: {},
      bcrumb: [
        {url: routerSvc.getUrl('UsersList'), label: 'Users'}
      ]
    });

    watchEffect(
      () => {
        ctx.isUpdateAllowed = userResources.isUpdateAllowed();
        ctx.pfuAllowed = userResources.isProfileUpdateAllowed(props.userId);
        userSvc.getUserById(+props.userId).then(user => ctx.user = user);
      }
    );

    return { ctx, userResources };
  },

  methods: {
  }
}
</script>
