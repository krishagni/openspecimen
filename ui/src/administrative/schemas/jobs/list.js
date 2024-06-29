
import i18n      from '@/common/services/I18n.js';
import jobsSvc   from '@/administrative/services/ScheduledJob.js';
import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      "name": "job.name",
      "captionCode": "jobs.title",
      "value": ({job}) => '#' + job.id + ' ' + job.name,
      "href": (row) => routerSvc.getUrl('JobAddEdit', {jobId: row.rowObject.job.id})
    },
    {
      "name": "job.createdBy",
      "captionCode": "jobs.created_by",
      "type": "user"
    },
    {
      "name": "job.type",
      "captionCode": "jobs.type",
      "value": ({job}) => i18n.msg('jobs.types.' + job.type)
    },
    {
      "name": "job.repeatSchedule",
      "captionCode": "jobs.repeat_interval",
      "value": ({job}) => jobsSvc.getScheduledDescription(job)
    },
    {
      "name": "job.lastRunOn",
      "captionCode": "jobs.last_run_time",
      "type": "date-time"
    }
  ],

  filters: [
    {
      name: 'query',
      type: 'text',
      captionCode: 'jobs.title'
    },
    {
      name: 'type',
      type: 'dropdown',
      captionCode: 'jobs.type',
      listSource: {
        displayProp: 'label',
        selectProp: 'name',
        options: [
          { name: 'INTERNAL', label: i18n.msg('jobs.types.INTERNAL') },
          { name: 'QUERY', label: i18n.msg('jobs.types.QUERY') }
        ]
      }
    }
  ]
}
