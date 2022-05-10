<template>
  <os-page>
    <os-page-head>
      <span>
        <h3>Container Tasks</h3>
      </span>

      <template #right>
        <os-list-size
          :list="ctx.tasks"
          :page-size="ctx.pageSize"
          :list-size="ctx.tasksCount"
          @updateListSize="getTasksCount"
        />
      </template>
    </os-page-head>

    <os-page-body>
      <os-page-toolbar>
        <template #default>
          <os-button left-icon="plus" label="Create" @click="createTask" v-if="ctx.allowEdits" />

          <os-button left-icon="box-open" label="Containers" @click="viewContainers" />
        </template>

        <template #right>
          <os-button left-icon="search" label="Search" @click="openSearch" />
        </template>
      </os-page-toolbar>

      <os-list-view
        :data="ctx.tasks"
        :schema="listSchema"
        :query="ctx.query"
        :allowSelection="false"
        :loading="ctx.loading"
        @filtersUpdated="loadTasks"
        @rowClicked="onTaskRowClick"
        showRowActions="ctx.allowEdits"
        ref="listView">

        <template #rowActions="slotProps">
          <os-button-group>
            <os-button size="small" left-icon="archive" v-os-tooltip.bottom="'Archive'"
              @click="confirmArchiveTask(slotProps.rowObject)" />
          </os-button-group>
        </template>
      </os-list-view>
    </os-page-body>
  </os-page>

  <os-confirm ref="confirmArchiveTaskDialog">
    <template #title>
      <span>Confirm Archive Task</span>
    </template>
    <template #message>
      <span>Are you sure you want to archive the container maintenance task: <b>{{ctx.toArchive.name}}</b>?</span>
    </template>
  </os-confirm>
</template>

<script>

import listSchema   from '@/administrative/schemas/container-tasks/list.js';

import containerSvc from '@/administrative/services/Container.js';
import routerSvc    from '@/common/services/Router.js';

export default {
  props: ['filters'],

  data() {
    return {
      ctx: {
        tasks: [],
        tasksCount: -1,
        loading: true,
        query: this.filters,
      },

      listSchema,
    };
  },

  created() {
    this.ctx.allowEdits = this.$ui.currentUser.admin || this.$ui.currentUser.instituteAdmin;
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadTasks: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize     = pageSize;

      await this.reloadTasks();
      routerSvc.goto('ContainerTasksList', {}, {filters: uriEncoding});
      return this.ctx.tasks;
    },

    reloadTasks: async function() {
      this.ctx.loading = true;
      const opts = {maxResults: this.ctx.pageSize};
      const tasks = await containerSvc.getTasks(Object.assign(opts, this.ctx.filterValues || {}));
      this.ctx.tasks = tasks.map(task => ({ task }));
      this.ctx.loading = false;
    },

    getTasksCount: async function() {
      const { count } = await containerSvc.getTasksCount({...this.ctx.filterValues});
      this.ctx.tasksCount = count;
    },

    onTaskRowClick: function({task}) {
      if (!this.ctx.allowEdits) {
        return;
      }

      routerSvc.goto('ContainerTaskAddEdit', {taskId: task.id});
    },

    createTask: function() {
      routerSvc.goto('ContainerTaskAddEdit', {taskId: -1})
    },

    viewContainers: function() {
      routerSvc.goto('ContainersList');
    },

    confirmArchiveTask: function({task}) {
      this.ctx.toArchive = task;
      this.$refs.confirmArchiveTaskDialog.open().then(
        (resp) => {
          if (resp != 'proceed') {
            return;
          }

          containerSvc.archiveTask(task).then(() => this.reloadTasks());
        }
      );
    }
  }
}
</script>
