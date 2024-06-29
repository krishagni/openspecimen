<template>
  <os-screen>
    <os-screen-panel :width="12">
      <os-page>
        <os-page-head>
          <span>
            <h3 v-t="'jobs.list'">Scheduled Jobs</h3>
          </span>

          <template #right>
            <os-list-size
              :list="ctx.jobs"
              :page-size="ctx.pageSize"
              :list-size="ctx.jobsCount"
              @updateListSize="getJobsCount"
            />
          </template>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar>
            <template #default>
              <os-button left-icon="plus" :label="$t('common.buttons.create')"
                @click="$goto('JobAddEdit', {jobId: -1}, {})" v-show-if-allowed="jobResources.createOpts" />

              <os-button-link left-icon="question-circle" :label="$t('common.buttons.help')"
                url="https://help.openspecimen.org/jobs" new-tab="true" />
            </template>

            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :context="ctx.ui"
            :data="ctx.jobs"
            :schema="listSchema"
            :query="ctx.query"
            :loading="ctx.loading"
            :showRowActions="true"
            @filtersUpdated="loadJobs"
            @rowClicked="onJobRowClick"
            ref="listView">
            <template #rowActions="slotProps">
              <os-button-group>
                <os-button left-icon="play" size="small" v-os-tooltip.bottom="$t('jobs.execute_job')"
                  @click="executeJob(slotProps.rowObject)" />
                <os-menu icon="ellipsis-h" :options="jobOptions(slotProps.rowObject.job)" />
              </os-button-group>
            </template>
          </os-list-view>

        </os-page-body>
      </os-page>

      <os-confirm-delete ref="deleteDialog" :captcha="false">
        <template #message>
          <span v-t="{path: 'jobs.confirm_delete', args: ctx.toDeleteJob}">Are you sure you want to delete the job: {ctx.toDeleteJob.name}?</span>
        </template>
      </os-confirm-delete>

      <os-dialog ref="jobArgsDialog">
        <template #header>
          <span>{{ctx.toRun.name}}</span>
        </template>
        <template #content>
          <os-form ref="jobArgsForm" :schema="jobArgsSchema" :data="ctx">
            <template v-slot:[`runArgs.args`]>
              <i>{{ctx.toRun.rtArgsHelpText}}</i>
            </template>
          </os-form>
        </template>
        <template #footer>
          <os-button text :label="$t('common.buttons.cancel')" @click="cancelRun" />
          <os-button primary :label="$t('jobs.run_job')" @click="runJob(ctx.toRun, ctx.runArgs)" />
        </template>
      </os-dialog>
    </os-screen-panel>
  </os-screen>
</template>

<script>

import listSchema from '@/administrative/schemas/jobs/list.js';

import alertSvc   from '@/common/services/Alerts.js';
import authSvc    from '@/common/services/Authorization.js';
import jobsSvc    from '@/administrative/services/ScheduledJob.js';
import routerSvc  from '@/common/services/Router.js';

import jobResources from './Resources.js';

export default {
  props: ['jobId', 'filters'],

  data() {
    return {
      ctx: {
        ui: this.$ui,

        jobs: [],

        jobsCount: -1,

        loading: true,

        query: this.filters,

        toRun: {},

        runArgs: {}
      },

      listSchema,

      jobResources,

      jobArgsSchema: {
        rows: [
          {
            "fields": [
              {
                "name": "runArgs.args",
                "labelCode": "jobs.runtime_parameters",
                "type": "textarea",
                "validations": {
                  "required": {
                    "messageCode": "jobs.runtime_parameters_req"
                  }
                }
              }
            ]
          }
        ]
      }
    };
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadJobs: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;

      this.ctx.loading = true;
      let opts = Object.assign({maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      jobsSvc.getJobs(opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.jobs = resp.map(job => ({job}));
        routerSvc.goto('JobsList', {}, {filters: uriEncoding});
        return this.ctx.jobs;
      });
    },

    getJobsCount: function() {
      this.ctx.jobsCount = -1;
      let opts = Object.assign({}, this.ctx.filterValues);
      jobsSvc.getJobsCount(opts).then(resp => this.ctx.jobsCount = resp.count);
    },

    jobOptions: function(job) {
      const options = [];
      if (authSvc.isAllowed(jobResources.updateOpts)) {
        options.push({
          icon: 'edit',
          caption: this.$t('common.buttons.edit'),
          onSelect: () => this.editJob(job)
        });
      }

      if (authSvc.isAllowed(jobResources.deleteOpts)) {
        options.push({
          icon: 'trash',
          caption: this.$t('common.buttons.delete'),
          onSelect: () => this.deleteJob(job)
        });
      }

      options.push({
        icon: 'list',
        caption: this.$t('jobs.view_runs'),
        onSelect: () => this.viewJobRuns(job)
      });

      return options;
    },

    onJobRowClick: function({job}) {
      this.editJob(job);
    },

    executeJob: function({job}) {
      if (!job.rtArgsProvided) {
        this.runJob(job);
        return;
      }

      this.ctx.toRun = job;
      this.ctx.runArgs = {};
      this.$refs.jobArgsDialog.open();
    },

    runJob: function(job, rtArgs) {
      jobsSvc.runJob(job, rtArgs && rtArgs.args).then(
        () => {
          alertSvc.success({code: 'jobs.job_exec_queued', args: job});
          this.$refs.jobArgsDialog.close();
        }
      );
    },

    cancelRun: function() {
      this.$refs.jobArgsDialog.close();
    },

    editJob: function(job) {
      routerSvc.goto('JobAddEdit', {jobId: job.id});
    },

    deleteJob: function(job) {
      this.ctx.toDeleteJob = job;
      this.$refs.deleteDialog.open().then(
        () => {
          jobsSvc.deleteJob(job).then(
            () => {
              alertSvc.success({code: 'jobs.deleted', args: job});
              this.$refs.listView.reload();
            }
          );
        }
      );
    },

    viewJobRuns: function(job) {
      routerSvc.goto('JobRunsList', {jobId: job.id});
    }
  }
}
</script>
