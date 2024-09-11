import http from '@/common/services/HttpClient.js';
import i18n from '@/common/services/I18n.js';

class ImportJob {
  getJobs(crit) {
    return http.get('import-jobs', crit);
  }

  getJobDescription(job) {
    return '#' + job.id + ' ' + i18n.msg('import.ops.' + job.type) + ' ' + i18n.msg('import.object_types.' + job.name)
  }

  createJob(job) {
    return http.post('import-jobs', job);
  }

  stopJob(job) {
    return http.put('import-jobs/' + job.id + '/stop');
  }

  downloadOutputFile(job) {
    return http.downloadFile(http.getUrl('import-jobs/' + job.id + '/output'));
  }
}

export default new ImportJob();
