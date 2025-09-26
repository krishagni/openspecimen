<template>
  <os-screen>
    <os-screen-panel :width="ctx.detailView ? 3 : 12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'roles.list'">Roles</h3>
          </span>

          <template #right>
            <os-button v-if="ctx.detailView"
              size="small" left-icon="expand-alt"
              v-os-tooltip.bottom="$t('common.switch_to_table_view')"
              @click="showTable" />

            <os-list-size v-else
              :list="ctx.roles"
              :page-size="ctx.pageSize"
              :list-size="ctx.rolesCount"
              @updateListSize="getRolesCount" />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar v-if="!ctx.detailView">
            <template #default>
              <span v-show-if-allowed="'admin'">
                <os-button left-icon="plus" :label="$t('common.buttons.create')"
                  @click="$goto('UserRoleAddEdit', {roleId: -1}, {})" />
              </span>

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://openspecimen.atlassian.net/wiki/x/boB1" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :context="ctx.ui"
            :data="ctx.roles"
            :schema="listSchema"
            :query="ctx.query"
            :loading="ctx.loading"
            :selected="ctx.selectedRole"
            @filtersUpdated="loadRoles"
            @rowClicked="onRoleRowClick"
            ref="listView" />
        </os-page-body>
      </os-page>
    </os-screen-panel>

    <os-screen-panel :width="9" v-if="$route.params && $route.params.roleId > 0 && ctx.selectedRole">
      <router-view :role-id="ctx.selectedRole.role.id" :key="$route.params.roleId" />
    </os-screen-panel>
  </os-screen>
</template>

<script>

import listSchema from '@/administrative/schemas/roles/list.js';

import routerSvc  from '@/common/services/Router.js';
import roleSvc    from '@/administrative/services/UserRole.js';

export default {
  props: ['roleId', 'filters'],

  data() {
    return {
      ctx: {
        ui: this.$ui,

        roles: [],

        rolesCount: -1,

        loading: true,

        query: this.filters,

        detailView: false,

        selectedRole: null
      },

      listSchema
    };
  },

  watch: {
    'roleId': function(newValue, oldValue) {
      if (newValue == oldValue) {
        return;
      }

      if (newValue > 0) {
        // TODO:
        let selectedRow = this.ctx.roles.find(({role}) => role.id == this.roleId);
        if (!selectedRow) {
          selectedRow = {role: {id: this.roleId}};
        }

        this.showDetails(selectedRow);
      } else {
        this.showTable(newValue == -2);
      }
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadRoles: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;

      const roles = await this.reloadRoles();
      if (this.roleId <= 0) {
        routerSvc.goto('UserRolesList', {roleId: -1}, {filters: uriEncoding});
      } else {
        // TODO:
        let selectedRow = roles.find(({role}) => role.id == this.roleId);
        if (!selectedRow) {
          selectedRow = {role: {id: this.roleId}};
        }

        this.showDetails(selectedRow);
      }
    },

    reloadRoles: function() {
      this.ctx.loading = true;
      let opts = Object.assign({maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      return roleSvc.getRoles(opts).then(
        roles => {
          this.ctx.loading = false;
          this.ctx.roles = roles.map(role => ({role}));
          return this.ctx.roles;
      });
    },

    getRolesCount: function() {
      this.ctx.rolesCount = -1;
      let opts = Object.assign({}, this.ctx.filterValues);
      roleSvc.getRolesCount(opts).then(({count}) => this.ctx.rolesCount = count);
    },

    onRoleRowClick: function({role}) {
      routerSvc.goto('UserRolesListItemDetail.Overview', {roleId: role.id}, {filters: this.filters});
    },

    showDetails: function(rowObject) {
      this.ctx.selectedRole = rowObject;
      if (!this.ctx.detailview) {
        this.ctx.detailView = true;
        this.$refs.listView.switchToSummaryView();
      }
    },

    showTable: function(reload) {
      this.ctx.detailView = false;
      this.$refs.listView.switchToTableView();
      routerSvc.goto('UserRolesList', {roleId: -1}, {filters: this.filters});
      if (reload) {
        this.$refs.listView.reload();
      }
    }
  }
}
</script>
