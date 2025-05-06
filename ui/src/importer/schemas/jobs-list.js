import http from '@/common/services/HttpClient.js';
import i18n from '@/common/services/I18n.js';
import jobSvc from '@/importer/services/ImportJob.js';

export default {
  key: "job.id",

  columns: [
    {
      name: "job.id",
      captionCode: "import.job_id",
      href: (row) => http.getUrl('import-jobs/' + row.rowObject.job.id + '/output'),
      value: ({job}) => jobSvc.getJobDescription(job)
    },
    {
      name: "job.createdBy",
      captionCode: "import.submitted_by",
      type: "user"
    },
    {
      name: "job.creationTime",
      captionCode: "import.submit_time",
      type: "date-time"
    },
    {
      name: "job.endTime",
      captionCode: "import.finish_time",
      type: "date-time"
    },
    {
      name: "job.success",
      captionCode: "import.success",
      value: ({job}) => job.totalRecords - job.failedRecords
    },
    {
      name: "job.failedRecords",
      captionCode: "import.failed"
    },
    {
      name: "job.status",
      captionCode: "import.status",
      value: ({job}) => i18n.msg("import.statuses." + job.status)
    }
  ]
}
