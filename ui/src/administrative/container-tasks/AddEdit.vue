<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.task.id">Create Container Task</h3>
        <h3 v-else>Update {{dataCtx.task.name}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <div v-if="ctx.loading">
        <os-message type="info">
          <span>Loading the form. Please wait for a moment...</span>
        </os-message>
      </div>
      <div v-else>
        <os-form ref="taskForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
          <div>
            <os-button primary :label="!dataCtx.task.id ? 'Create' : 'Update'" @click="saveOrUpdate"
              v-show-if-allowed="'institute-admin'" />

            <os-button text label="Cancel"  @click="cancel" />
          </div>
        </os-form>
      </div>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertsSvc     from '@/common/services/Alerts.js';
import routerSvc     from '@/common/services/Router.js';
import containerSvc  from '@/administrative/services/Container.js';

export default {
  props: ['taskId'],

  inject: ['ui'],

  setup() {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('ContainerTasksList', {}), label: 'Container Types'}
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
      alertsSvc.success('Container task ' + savedTask.name + (task.id ? ' updated!' : ' created!')); 
      routerSvc.goto('ContainerTasksList');
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
