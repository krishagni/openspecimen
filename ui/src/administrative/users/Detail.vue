<template>
  <os-page v-if="userId > 0">
    <os-page-head :noNavButton="listDetailView == true">
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" v-show-if-allowed="userResources.readOpts" />
      </template>

      <span class="os-title">
        <h3>{{$filters.username(ctx.user)}}</h3>
        <div class="accessories" v-if="ctx.user && ctx.user.id > 0">
          <os-tag v-if="status.caption" :value="status.caption" :rounded="true" :type="status.type" />
          <os-copy-link size="small" :route="{name: 'UserDetail.Overview', params: {userId: ctx.user.id}}" />
          <os-new-tab   size="small" :route="{name: 'UserDetail.Overview', params: {userId: ctx.user.id}}" />
        </div>
      </span>
    </os-page-head>
    <os-page-body>
      <div>
        <os-tab-menu v-if="listDetailView">
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>
            <li v-if="ctx.isUpdateAllowed && ctx.user.type == 'NONE'">
              <router-link :to="getRoute('Roles')">
                <span v-t="'users.roles'">Roles</span>
              </router-link>
            </li>
            <li v-if="ctx.isUpdateAllowed">
              <router-link :to="getRoute('Forms.List')">
                <span v-t="'users.forms'">Forms</span>
              </router-link>
            </li>
            <li v-if="ctx.pfuAllowed">
              <router-link :to="getRoute('ProfileForms.List')">
                <span v-t="'users.profile_forms'">Profile Forms</span>
              </router-link>
            </li>

            <os-plugin-views page="user-detail" view="tab-menu" :query="query" />
          </ul>
        </os-tab-menu>
        <os-side-menu v-else>
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <os-icon name="eye" />
                <span class="label" v-t="'common.overview'">Overview</span>
              </router-link>
            </li>
            <li v-if="ctx.isUpdateAllowed && ctx.user.type == 'NONE'">
              <router-link :to="getRoute('Roles')">
                <os-icon name="users" />
                <span class="label" v-t="'users.roles'">Roles</span>
              </router-link>
            </li>
            <li v-if="ctx.isUpdateAllowed">
              <router-link :to="getRoute('Forms.List')">
                <os-icon name="copy" />
                <span class="label" v-t="'users.forms'">Forms</span>
              </router-link>
            </li>
            <li v-if="ctx.pfuAllowed">
              <router-link :to="getRoute('ProfileForms.List')">
                <os-icon name="user" />
                <span class="label" v-t="'users.profile_forms'">Profile Forms</span>
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

import i18n          from '@/common/services/I18n.js';
import routerSvc     from '@/common/services/Router.js';
import userSvc       from '@/administrative/services/User.js';
import userResources from '@/administrative/users/Resources.js';

export default {
  name: 'UserDetail',

  props: ['userId', 'listDetailView'],

  setup() {
    const ctx = reactive({
      user: {},
      bcrumb: [
        {url: routerSvc.getUrl('UsersList', {userId: -1}), label: i18n.msg('users.list')}
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

  computed: {
    status: function() {
      if (!this.ctx.user || this.ctx.user.activityStatus == 'Active') {
        return {caption: undefined};
      }

      switch (this.ctx.user.activityStatus) {
        case 'Closed':
          return {caption: i18n.msg('users.status.archived'), type: 'danger'};

        case 'Expired':
          return {caption: i18n.msg('users.status.password_expired'), type: 'danger'};

        case 'Locked':
          return {caption: i18n.msg('users.status.locked'), type: 'danger'};

        case 'Pending':
          return {caption: i18n.msg('users.status.pending'), type: 'warning'};
      }

      return {caption: undefined};
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
