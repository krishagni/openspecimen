<template>
  <Page>
    <PageHeader>
      <span>
        <h3>User Groups</h3>
      </span>

      <template #right>
        <ListSize
          :list="ctx.userGroups"
          :page-size="ctx.pageSize"
          :list-size="ctx.groupsCount"
          @updateListSize="getGroupsCount"
        />
      </template>
    </PageHeader>
    <PageBody>
      <PageToolbar>
        <template #default>
          <Button left-icon="user" label="Users" @click="$goto('UsersList')" />
        </template>

        <template #right>
          <Button left-icon="search" label="Search" @click="openSearch" />
        </template>
      </PageToolbar>

      <ListView
        :data="ctx.userGroups"
        :columns="listSchema.columns"
        :filters="listSchema.filters"
        :query="ctx.query"
        :allowSelection="false"
        :showRowActions="updateAllowed"
        :loading="ctx.loading"
        @filtersUpdated="loadGroups"
        @rowClicked="showGroupDetails"
        ref="listView"
      >
        <template #rowActions="{rowObject}">
          <ButtonGroup>
            <Button left-icon="edit"   size="small" @click="editGroup(rowObject)"   />
            <Button left-icon="trash"  size="small" @click="deleteGroup(rowObject)" />
          </ButtonGroup>
        </template>
      </ListView>

      <DeleteObject ref="deleteGroup" :input="ctx.deleteGroupOpts" />
    </PageBody>
  </Page>
</template>

<script>

import { reactive } from 'vue';

import Page from '@/common/components/Page.vue';
import PageHeader from '@/common/components/PageHeader.vue';
import PageBody from '@/common/components/PageBody.vue';
import PageToolbar from '@/common/components/PageToolbar.vue';
import Button from '@/common/components/Button.vue';
import ButtonGroup from '@/common/components/ButtonGroup.vue';
import ListView from '@/common/components/ListView.vue';
import ListSize from '@/common/components/ListSize.vue';
import DeleteObject from '@/common/components/DeleteObject.vue';

import listSchema from '@/administrative/user-groups/schemas/list.js';

import authSvc    from '@/common/services/Authorization.js';
import routerSvc  from '@/common/services/Router.js';
import userGrpSvc from '@/administrative/services/UserGroup.js';

export default {
  props: ['filters'],

  components: {
    Page,
    PageHeader,
    PageBody,
    PageToolbar,
    Button,
    ButtonGroup,
    ListView,
    ListSize,
    DeleteObject
  },

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
      userGrpSvc.getUserGroups(opts).then(resp => {
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
      routerSvc.goto('UsersList', {}, {groupId: group.id});
    },

    editGroup: function(group) {
      routerSvc.goto('UserGroupAddEdit', {groupId: group.id}, {});
    },

    deleteGroup: function(group) {
      Object.assign(
        this.ctx.deleteGroupOpts,
        {
          type: 'User Group',
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
