<template>
  <os-page>
    <os-page-head :noNavButton="true">
      <span class="os-title">
        <h3>{{ctx.role.name}}</h3>
        <div class="accessories" v-if="ctx.role && ctx.role.id > 0">
          <os-copy-link size="small"
            :route="{name: 'UserRolesListItemDetail.Overview', params: {roleId: ctx.role.id}}" />
        </div>
      </span>
    </os-page-head>

    <os-page-body>
      <div>
        <os-tab-menu>
          <ul>
            <li>
              <router-link :to="getRoute('Overview')">
                <span v-t="'common.overview'">Overview</span>
              </router-link>
            </li>
          </ul>
        </os-tab-menu>

        <router-view :role="ctx.role" v-if="ctx.role.id" />
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import roleSvc from '@/administrative/services/UserRole.js';

export default {
  props: ['roleId'],

  data() {
    return {
      ctx : {
        role: {},
      }
    };
  },

  created() {
    this.query = {};
    if (this.$route.query) {
      Object.assign(this.query, {filters: this.$route.query.filters});
    }

    this.loadRole();
  },

  watch: {
    roleId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this.loadRole();
      }
    }
  },

  methods: {
    loadRole: async function() {
      this.ctx.role = await roleSvc.getRole(+this.roleId);
    },

    getRoute: function(routeName, params, query) {
      return {
        name: 'UserRolesListItemDetail.' + routeName,
        params: params,
        query: {...this.query, query}
      }
    }
  }
}
</script>
