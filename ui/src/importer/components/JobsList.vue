<template>
  <div>
    <div class="toolbar">
      <os-button left-icon="sync" :label="$t('common.buttons.refresh')" @click="loadJobs(0)" />
    </div>
    <os-list-view
      :data="ctx.jobs" :schema="listSchema"
      :loading="ctx.loading" :showRowActions="true" ref="listView">

      <template #rowActions="{rowObject: {job}}">
        <os-button-group>
          <os-button size="small" left-icon="download" @click="downloadOutput(job)"
            v-os-tooltip.bottom="$t('import.download_status')" v-if="job.status != 'QUEUED'" />
          <os-button size="small" left-icon="ban" @click="stopJob(job)" 
            v-os-tooltip.bottom="$t('import.stop_job')" v-if="job.status == 'QUEUED' || job.status == 'IN_PROGRESS'" />
        </os-button-group>
      </template>

      <template #footer>
        <os-pager :start-at="ctx.startAt" :have-more="ctx.haveMoreJobs"
          @previous="previousPage" @next="nextPage" />
      </template>
    </os-list-view>

    <os-confirm ref="confirmStopJob">
      <template #title>
        <span v-t="{path: 'import.stop_job_q', args: ctx.job}">Stop Import Job #{id}</span>
      </template>
      <template #message>
        <span v-t="{path: 'import.confirm_stop_job', args: ctx.job}"></span>
      </template>
    </os-confirm>
  </div>
</template>

<script>
import listSchema from '@/importer/schemas/jobs-list.js';

import alertsSvc from '@/common/services/Alerts.js';
import importSvc from '@/importer/services/ImportJob.js';

const MAX_JOBS = 25;

export default {
  props: ['objectTypes', 'objectParams'],

  data() {
    return {
      ctx: {
        jobs: [],

        loading: true,

        startAt: 0,

        haveMoreJobs: true
      },

      listSchema
    };
  },

  created() {
    this.loadJobs(this.ctx.startAt); 
  },

  methods: {
    loadJobs: async function(startAt) {
      const crit = { objectType: this.objectTypes, startAt: startAt || 0, maxResults: MAX_JOBS + 1 };
      if (this.objectParams) {
        Object.assign(crit, this.objectParams);
      }

      this.ctx.loading = true;
      const jobs = await importSvc.getJobs(crit);
      if (jobs.length > MAX_JOBS) {
        this.ctx.haveMoreJobs = true;
        jobs.splice(jobs.length - 1, 1);
      } else {
        this.ctx.haveMoreJobs = false;
      }

      this.ctx.jobs = jobs.map(job => ({job}));
      this.ctx.loading = false;
      this.ctx.startAt = startAt;
    },

    reload: function() {
      this.loadJobs(0);
    },

    previousPage: function() {
      this.loadJobs(this.ctx.startAt - MAX_JOBS);
    },

    nextPage: function() {
      this.loadJobs(this.ctx.startAt + MAX_JOBS);
    },

    downloadOutput: function(job) {
      importSvc.downloadOutputFile(job);
    },

    stopJob: async function(job) {
      this.ctx.job = job;
      const resp = await this.$refs.confirmStopJob.open();
      if (resp != 'proceed') {
        return;
      }

      const {status} = await importSvc.stopJob(job);
      if (status == 'STOPPED') {
        alertsSvc.success({code: 'import.job_stopped', args: job});
      } else if (status == 'COMPLETED') {
        alertsSvc.success({code: 'import.job_completed', args: job});
      } else {
        alertsSvc.info({code: 'import.job_stop_in_progress', args: job});
      }
    }
  }
}
</script>

<style scoped>
.toolbar {
  margin-bottom: 1.25rem;
}
</style>
