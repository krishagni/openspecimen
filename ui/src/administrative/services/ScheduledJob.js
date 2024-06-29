
import http from '@/common/services/HttpClient.js';
import i18n from '@/common/services/I18n.js';

import addEditSchema from '@/administrative/schemas/jobs/addedit.js';

class ScheduledJob {
  async getJobs(filterOpts) {
    return http.get('scheduled-jobs', filterOpts || {});
  }

  async getJobsCount(filterOpts) {
    return http.get('scheduled-jobs/count', filterOpts || {});
  }

  async getJob(jobId) {
    return http.get('scheduled-jobs/' + jobId);
  }

  async saveOrUpdateJob(job) {
    if (job.id > 0) {
      return http.put('scheduled-jobs/' + job.id, job);
    } else {
      return http.post('scheduled-jobs', job);
    }
  }

  async deleteJob(job) {
    return http.delete('scheduled-jobs/' + job.id);
  }

  async runJob(job, args) {
    return http.post('scheduled-jobs/' + job.id + '/runs', {args});
  } 

  async getJobRuns(jobId, opts) {
    return http.get('scheduled-jobs/' + jobId + '/runs', opts);
  }

  async downloadResult({jobId, id: runId}) {
    return http.downloadFile(http.getUrl('scheduled-jobs/' + jobId + '/runs/' + runId + '/result-file'));
  }

  getScheduledDescription(job) {
    const key = 'jobs.repeat_interval_desc.' + job.repeatSchedule;
    const args = {...job};
    if (args.scheduledHour < 10) {
      args.scheduledHour = '0' + args.scheduledHour
    }

    if (args.scheduledMinute < 10) {
      args.scheduledMinute = '0' + args.scheduledMinute;
    }

    if (job.repeatSchedule == 'MONTHLY') {
      const day = job.scheduledDayOfMonth;
      args.dayOrd = i18n.msg('jobs.ordinals.' + ((day % 10) < 4 && (day < 10 || day > 19) ? day % 10 : 'oth'));
    } else if (job.repeatSchedule == 'WEEKLY') {
      args.weekDay = i18n.msg('jobs.week_days.' + job.scheduledDayOfWeek);
    } else if (job.repeatSchedule == 'HOURLY') {
      const minute = job.scheduledMinute;
      args.minuteOrd = i18n.msg('jobs.ordinals.' + ((minute % 10) < 4 && (minute < 10 || minute > 19) ? minute % 10 : 'oth'));
    } else if (job.repeatSchedule == 'MINUTELY') {
      args.count = job.minutelyInterval;
    }

    return i18n.msg(key, args);
  }

  getAddEditFormSchema() {
    return addEditSchema;
  }
}

export default new ScheduledJob();
