<template>
  <os-screen>
    <os-screen-panel :width="12">
      <os-page>
        <os-page-head>
          <template #breadcrumb>
            <os-breadcrumb :items="bcrumb" />
          </template>

          <span>
            <h3 v-t="{path: 'jobs.job_run_logs', args: ctx.job}">{{ctx.job.name}}</h3>
          </span>
        </os-page-head>

        <os-page-body>
          <os-page-toolbar>
            <template #right>
              <os-button left-icon="search" :label="$t('common.buttons.search')" @click="openSearch" />
            </template>
          </os-page-toolbar>

          <os-list-view
            :context="ctx.ui"
            :data="ctx.jobRuns"
            :schema="listSchema"
            :query="ctx.query"
            :loading="ctx.loading"
            :showRowActions="true"
            @filtersUpdated="loadJobRuns"
            ref="listView">
            <template #rowActions="slotProps">
              <os-button left-icon="download" size="small" v-os-tooltip.bottom="$t('common.buttons.download')"
                v-if="slotProps.rowObject.jobRun.status != 'IN_PROGRESS' && slotProps.rowObject.jobRun.logFilePath"
                @click="downloadResults(slotProps.rowObject)" />
            </template>
          </os-list-view>
        </os-page-body>
      </os-page>
    </os-screen-panel>
  </os-screen>
</template>

<script>

import listSchema from '@/administrative/schemas/jobs/runs-list.js';

import i18n       from '@/common/services/I18n.js';
import jobsSvc    from '@/administrative/services/ScheduledJob.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['jobId', 'filters'],

  data() {
    return {
      ctx: {
        ui: this.$ui,

        job: {},

        jobRuns: [],

        loading: true,

        query: this.filters,
      },

      listSchema,
    };
  },

  created() {
    jobsSvc.getJob(this.jobId).then(job => this.ctx.job = job);
  },

  computed: {
    bcrumb: function() {
      return [{url: routerSvc.getUrl('JobsList', {}), label: i18n.msg('jobs.list')}];
    }
  },

  methods: {
    openSearch: function() {
      this.$refs.listView.toggleShowFilters();
    },

    loadJobRuns: async function({filters, uriEncoding, pageSize}) {
      this.ctx.filterValues = filters;
      this.ctx.pageSize = pageSize;

      this.ctx.loading = true;
      let opts = Object.assign({maxResults: this.ctx.pageSize}, this.ctx.filterValues || {});
      jobsSvc.getJobRuns(this.jobId, opts).then(resp => {
        this.ctx.loading = false;
        this.ctx.jobRuns = resp.map(jobRun => ({jobRun}));
        routerSvc.goto('JobRunsList', {jobId: this.jobId}, {filters: uriEncoding});
        return this.ctx.jobRuns;
      });
    },

    downloadResults: function({jobRun}) {
      jobsSvc.downloadResult(jobRun);
    }
  }
}
</script>
