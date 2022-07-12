<template>
  <os-page>
    <os-page-head>
      <span>
        <h3 v-t="'user_groups.list'">User Groups</h3>
      </span>

      <template #right>
        <os-list-size
          :list="ctx.userGroups"
          :page-size="ctx.pageSize"
          :list-size="ctx.groupsCount"
          @updateListSize="getGroupsCount"
        />
      </template>
    </os-page-head>
    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-button left-icon="user" :label="$t('user_groups.users')" @click="$goto('UsersList', {userId: -1})" />
        </template>

        <template #right>
          <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.userGroups"
        :schema="listSchema"
        :query="ctx.query"
        :allowSelection="false"
        :showRowActions="updateAllowed"
        :loading="ctx.loading"
        @filtersUpdated="loadGroups"
        @rowClicked="showGroupDetails"
        ref="listView"
      >
        <template #rowActions="{rowObject}">
          <os-button-group>
            <os-button left-icon="edit"   size="small" @click="editGroup(rowObject)"   />
            <os-button left-icon="trash"  size="small" @click="deleteGroup(rowObject)" />
          </os-button-group>
        </template>
      </os-list-view>

      <os-delete-object ref="deleteGroup" :input="ctx.deleteGroupOpts" />
    </os-page-body>
  </os-page>
</template>

<script>

import { reactive } from 'vue';

import listSchema from '@/administrative/user-groups/schemas/list.js';

import authSvc    from '@/common/services/Authorization.js';
import routerSvc  from '@/common/services/Router.js';
import userGrpSvc from '@/administrative/services/UserGroup.js';

export default {
  props: ['filters'],

  setup(props) {
    let ctx = reactive({
      loading: true,
      userGroups: [],
      query: props.filters,
      pageSize: undefined,
      groupsCount: -1,
      deleteGroupOpts: { }
    });

    return { ctx, listSchema };
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadGroups: function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;

      routerSvc.goto('UserGroupsList', {}, {filters: uriEncoding});
      this.reloadGroups();
    },

    reloadGroups: function() {
      this.ctx.loading = true;
      let opts = Object.assign({includeStats: true, maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      userGrpSvc.getGroups(opts).then(resp => {
        this.ctx.loading    = false;
        this.ctx.userGroups = resp;
      });
    },

    getGroupsCount: function() {
      this.ctx.groupsCount = -1;
      let opts = Object.assign({}, this.ctx.filterValues);
      userGrpSvc.getGroupsCount(opts).then((resp) => this.ctx.groupsCount = resp.count);
    },

    showGroupDetails: function(group) {
      routerSvc.goto('UsersList', {userId: -1}, {groupId: group.id});
    },

    editGroup: function(group) {
      routerSvc.goto('UserGroupAddEdit', {groupId: group.id}, {});
    },

    deleteGroup: function(group) {
      Object.assign(
        this.ctx.deleteGroupOpts,
        {
          type: this.$t('user_groups.singular'),
          title: group.name,
          dependents: () => new Promise((resolve) => resolve([])),
          deleteObj: () => userGrpSvc.deleteGroup(group)
        }
      );

      let self = this;
      this.$refs.deleteGroup.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            self.reloadGroups();
          }
        }
      );
    }
  },

  computed: {
    updateAllowed: function() {
      return authSvc.isAllowed({resource: 'User', operations: ['Update']})
    }
  }
}
</script>
