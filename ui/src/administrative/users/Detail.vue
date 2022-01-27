<template>
  <os-page v-if="userId > 0">
    <os-page-head :noNavButton="listDetailView == true">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>{{ctx.user.firstName}} {{ctx.user.lastName}}</h3>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="listDetailView">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span>Overview</span>
              </router-link>
            </li>
            <li v-if="ctx.isUpdateAllowed && ctx.user.type == 'NONE'">
              <router-link :to="getRoute('Roles')">
                <span>Roles</span>
              </router-link>
            </li>
            <li v-if="ctx.isUpdateAllowed">
              <router-link :to="getRoute('Forms.List')">
                <span>Forms</span>
              </router-link>
            </li>
            <li v-if="ctx.pfuAllowed">
              <router-link :to="getRoute('ProfileForms.List')">
                <span>Profile Forms</span>
              </router-link>
            </li>

            <os-plugin-views page="user-detail" view="tab-menu" :query="query" />
          </ul>
        </os-tab-menu>
        <os-side-menu v-else>
          <ul>
            <li v-os-tooltip.right="'Overview'">
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
              </router-link>
            </li>
            <li v-if="ctx.isUpdateAllowed && ctx.user.type == 'NONE'" v-os-tooltip.right="'Roles'">
              <router-link :to="getRoute('Roles')">
                <os-icon name="users" />
              </router-link>
            </li>
            <li v-if="ctx.isUpdateAllowed" v-os-tooltip.right="'Forms'">
              <router-link :to="getRoute('Forms.List')">
                <os-icon name="copy" />
              </router-link>
            </li>
            <li v-if="ctx.pfuAllowed" v-os-tooltip.right="'Profile Forms'">
              <router-link :to="getRoute('ProfileForms.List')">
                <os-icon name="user" />
              </router-link>
            </li>

            <os-plugin-views page="user-detail" view="side-menu" :query="query" />
          </ul>
        </os-side-menu>

        <router-view :user="ctx.user" v-if="ctx.user.id"> </router-view>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive } from 'vue';

import routerSvc from '@/common/services/Router.js';
import userSvc from '@/administrative/services/User.js';
import userResources from '@/administrative/users/Resources.js';

export default {
  name: 'UserDetail',

  props: ['userId', 'listDetailView'],

  setup() {
    const ctx = reactive({
      user: {},
      bcrumb: [
        {url: routerSvc.getUrl('UsersList', {userId: -1}), label: 'Users'}
      ]
    });

    return { ctx, userResources };
  },

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    this.detailRouteName = route.name.substr(0, route.name.indexOf('.'));
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters, groupId: this.$route.query.groupId});
    }

    this.loadUser();
  },

  watch: {
    'userId': function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.loadUser();
    }
  },

  methods: {
    loadUser: async function() {
      const ctx = this.ctx;
      if (ctx.user && ctx.user.id == +this.userId) {
        return;
      }

      ctx.isUpdateAllowed = userResources.isUpdateAllowed();
      ctx.pfuAllowed      = userResources.isProfileUpdateAllowed(this.userId);
      ctx.user            = await userSvc.getUserById(+this.userId);
    },

    getRoute: function(routeName, params, query) {
      return {
        name: this.detailRouteName + '.' + routeName,
        params: params,
        query: {...this.query, query}
      }
    }
  }
}
</script>
