<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span>
        <h3>
         <span v-if="!dataCtx.job.id" v-t="'jobs.create_job'">Create Job</span>
         <span v-else v-t="{path: 'common.update', args: {name: dataCtx.job.name}}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="jobForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <template #static-fields>
          <div class="row" v-if="dataCtx.job.repeatSchedule">
            <div class="field">
              <os-label>
                <span v-t="'jobs.run_schedule'"></span>
              </os-label>
              <span>{{scheduledDescription}}</span>
            </div>
          </div>
        </template>
        <div>
          <os-button primary :label="$t(!dataCtx.job.id ? 'common.buttons.create' : 'common.buttons.update')"
            @click="saveOrUpdate" />
          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import alertSvc     from '@/common/services/Alerts.js';
import http         from '@/common/services/HttpClient.js';
import i18n         from '@/common/services/I18n.js';
import jobsSvc      from '@/administrative/services/ScheduledJob.js';
import routerSvc    from '@/common/services/Router.js';

export default {
  props: ['jobId', 'queryId'],

  data() {
    return {
      ctx: {
        addEditFs: {rows: []}
      },

      dataCtx: {
        job: {}
      }
    }
  },

  created() {
    const schema = jobsSvc.getAddEditFormSchema();
    this.ctx.addEditFs = schema.layout;

    this._loadJob(this.jobId);
  },

  computed: {
    bcrumb: function() {
      return [{url: routerSvc.getUrl('JobsList', {}), label: i18n.msg('jobs.list')}];
    },

    scheduledDescription: function() {
      return jobsSvc.getScheduledDescription(this.dataCtx.job);
    }
  },

  watch: {
    jobId: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._loadJob(newVal);
      }
    }
  },

  methods: {
    handleInput: function(event) {
      Object.assign(this.dataCtx, event.data);
    },

    saveOrUpdate: function() {
      if (!this.$refs.jobForm.validate()) {
        return;
      }

      jobsSvc.saveOrUpdateJob(this.dataCtx.job).then(
        savedJob => {
          alertSvc.success(this.$t('jobs.saved', savedJob));
          routerSvc.goto('JobsList', {}, {});
        }
      );
    },

    cancel: function() {
      routerSvc.goto('JobsList', {}, {});
    },

    _loadJob: function(jobId) {
      if (jobId > 0) {
        jobsSvc.getJob(jobId).then(job => this.dataCtx.job = job);
      } else {
        if (this.queryId > 0) {
          http.get('saved-queries/' + this.queryId).then(
            query => {
              this.dataCtx.job = {
                name: query.title,
                type: 'QUERY',
                savedQuery: query,
                repeatSchedule: 'ONDEMAND',
                startDate: Date.now()
              }
            }
          );
        } else {
          this.dataCtx.job = {
            type: 'INTERNAL',
            repeatSchedule: 'ONDEMAND',
            startDate: Date.now()
          };
        }
      }
    }
  }
}
</script>
