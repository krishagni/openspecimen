import JobsList  from './JobsList.vue';
import CreateJob from './CreateJob.vue';

export default {
  install(app) {
    app.component('os-import-jobs-page',    JobsList);
    app.component('os-import-records-page', CreateJob);
  }
}
