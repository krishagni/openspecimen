
import i18n      from '@/common/services/I18n.js';

export default {
  columns: [
    {
      "name": "jobRun.id",
      "captionCode": "jobs.run_id",
      "value": ({jobRun}) => '#' + jobRun.id
    },
    {
      "name": "jobRun.runBy",
      "captionCode": "jobs.run_by",
      "type": "user"
    },
    {
      "name": "jobRun.startedAt",
      "captionCode": "jobs.run_start_time",
      "type": "date-time"
    },
    {
      "name": "jobRun.finishedAt",
      "captionCode": "jobs.run_end_time",
      "type": "date-time"
    },
    {
      "name": "jobRun.status",
      "captionCode": "jobs.run_status",
      "value": ({jobRun}) => i18n.msg('jobs.run_statuses.' + jobRun.status)
    }
  ],

  filters: [
    {
      name: 'fromDate',
      type: 'date',
      captionCode: 'jobs.run_start_time',
      showTime: true,
      format: 'yyyy-MM-dd HH:mm'
    },
    {
      name: 'toDate',
      type: 'date',
      captionCode: 'jobs.run_end_time',
      showTime: true,
      format: 'yyyy-MM-dd HH:mm'
    },
    {
      name: 'status',
      type: 'dropdown',
      captionCode: 'jobs.run_status',
      listSource: {
        displayProp: 'label',
        selectProp: 'name',
        options: [
          { name: 'IN_PROGRESS', label: i18n.msg('jobs.run_statuses.IN_PROGRESS') },
          { name: 'SUCCEEDED', label: i18n.msg('jobs.run_statuses.SUCCEEDED') },
          { name: 'FAILED', label: i18n.msg('jobs.run_statuses.FAILED') }
        ]
      }
    }
  ]
}
