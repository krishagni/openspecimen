
<template>
  <div>
    <component :ref="taskRef" :is="ctx.task.component"
      :def="ctx.task.configuration" :context="ctx" :task="ctx.task"
      :items="ctx.items" :require-data="ctx.requiredData"
      @next-task="next" @vue:mounted="setActionButtons" @vue:updated="setActionButtons" />
  </div>
</template>

<script>

import http from '@/common/services/HttpClient.js'

export default {
  props: ['taskName', 'cp', 'item'],

  data() {
    return {
      ctx: {
        cp: this.cp,

        task: {},

        items: [this.item]
      }
    };
  },

  created() {
    this._loadTask();
  },

  methods: {
    _loadTask: function() {
      const [libraryName, taskName] = this.taskName.replace(/\s+/g, '').split(':');
      http.get('task-libraries/tasks', {libraryName, taskName}).then(
        task => {
          task.component = 'os-' + task.type + '-task';
          if (!task.jsonConfig && task.configuration) {
            try {
              task.configuration = this.$osSvc.exprUtil.evalJavaScript('return (' + task.configuration + ')');
            } catch (e) {
              console.log(e);
              this.$osSvc.alertsSvc.error({code: 'workflows.task_js_parse_error'});
            }
          }
          this.ctx.task = task
        }
      );
    }
  }
}
</script>
