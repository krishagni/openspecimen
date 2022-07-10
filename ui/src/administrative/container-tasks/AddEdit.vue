<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.task.id">
          <span v-t="'container_tasks.create'">Create Container Task</span>
        </h3>
        <h3 v-else>
          <span v-t="{path: 'common.update', args: dataCtx.task}">Update {{dataCtx.task.name}}</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="ctx.loading">
        <os-message type="info">
          <span v-t="'common.loading_form'">Loading the form. Please wait for a moment...</span>
        </os-message>
      </div>
      <div v-else>
        <os-form ref="taskForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
          <div>
            <os-button primary :label="$t(!dataCtx.task.id ? 'common.buttons.create' : 'common.buttons.update')"
              @click="saveOrUpdate" v-show-if-allowed="'institute-admin'" />

            <os-button text :label="$t('common.buttons.cancel')"  @click="cancel" />
          </div>
        </os-form>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertsSvc     from '@/common/services/Alerts.js';
import i18n          from '@/common/services/I18n.js';
import routerSvc     from '@/common/services/Router.js';
import containerSvc  from '@/administrative/services/Container.js';

export default {
  props: ['taskId'],

  inject: ['ui'],

  setup() {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('ContainerTasksList', {}), label: i18n.msg('container_tasks.list')}
      ],

      addEditFs: {rows: []},

      loading: true,
    });

    let dataCtx = reactive({
      task: {},

      currentUser: ui.currentUser,
    });

    return { ctx, dataCtx };
  },

  created: async function() {
    const { schema } = containerSvc.getTaskAddEditFormSchema();
    this.ctx.addEditFs = schema;
    this.loadTask();
  },

  watch: {
    taskId: function (newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      this.loadTask();
    }
  },

  methods: {
    loadTask: async function() {
      const ctx        = this.ctx;
      const dataCtx    = this.dataCtx;

      ctx.loading = true;
      dataCtx.task = { };
      if (this.taskId && +this.taskId > 0) {
        dataCtx.task = await containerSvc.getTask(this.taskId);
      }

      ctx.loading = false;
    },

    handleInput: async function({data}) {
      Object.assign(this.dataCtx, data);
    },

    saveOrUpdate: async function() {
      const task = this.dataCtx.task;
      if (!this.$refs.taskForm.validate()) {
        return;
      }

      const savedTask = await containerSvc.saveOrUpdateTask(task);
      alertsSvc.success({code: task.id ? 'container_tasks.updated' : 'container_tasks.created', args: savedTask});
      routerSvc.goto('ContainerTasksList');
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
