
import i18n from '@/common/services/I18n.js';

export default {
  layout: {
    "rows": [
      {
        "fields": [
          {
            "name": "job.name",
            "labelCode": "jobs.name",
            "type": "text",
            "validations": {
              "required": {
                "messageCode": "jobs.name_req"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.type",
            "labelCode": "jobs.type",
            "type": "radio",
            "options": [
              { value: 'INTERNAL', captionCode: 'jobs.types.INTERNAL' },
              { value: 'QUERY', captionCode: 'jobs.types.QUERY' }
            ],
            "optionsPerRow": 2,
            "validations": {
              "required": {
                "messageCode": "jobs.type_req"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.sharedWith",
            "labelCode": "jobs.shared_with_users",
            "type": "user",
            "multiple": true
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.taskImplFqn",
            "labelCode": "jobs.task_implementation",
            "type": "text",
            "validations": {
              "required": {
                "messageCode": "jobs.task_impl_req"
              }
            },
            "showWhen": "job.type != 'QUERY'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.savedQuery",
            "labelCode": "jobs.query",
            "type": "dropdown",
            "listSource": {
              "apiUrl": "saved-queries",
              "displayProp": "title",
              "listAttr": "queries"
            },
            "validations": {
              "required": {
                "messageCode": "jobs.query_req"
              }
            },
            "showWhen": "job.type == 'QUERY'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.fixedArgs",
            "labelCode": "jobs.fixed_parameters",
            "type": "textarea",
            "showWhen": "job.type == 'INTERNAL'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.rtArgsProvided",
            "labelCode": "jobs.runtime_parameters",
            "type": "radio",
            "options": [
              { value: true, captionCode: 'jobs.rt_parameters_provided' },
              { value: false, captionCode: 'jobs.rt_parameters_not_provided' }
            ],
            "optionsPerRow": 2,
            "validations": {
              "required": {
                "messageCode": "jobs.runtime_parameters_req"
              }
            },
            "showWhen": "job.type == 'INTERNAL'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.rtArgsHelpText",
            "labelCode": "jobs.help_text",
            "type": "textarea",
            "validations": {
              "required": {
                "messageCode": "jobs.help_text_req"
              }
            },
            "showWhen": "job.type == 'INTERNAL' && job.rtArgsProvided"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.runAs",
            "labelCode": "jobs.run_as_user",
            "type": "user",
            "showWhen": "job.type == 'QUERY'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.recipients",
            "labelCode": "jobs.notify_users",
            "type": "user",
            "multiple": true
          }
        ]
      },
      {
        "fields": [
          {
            "name": "job.repeatSchedule",
            "labelCode": "jobs.repeat_interval",
            "type": "radio",
            "options": [
              { "value": "MONTHLY", "captionCode": "jobs.repeat_intervals.MONTHLY" },
              { "value": "WEEKLY", "captionCode": "jobs.repeat_intervals.WEEKLY" },
              { "value": "DAILY", "captionCode": "jobs.repeat_intervals.DAILY" },
              { "value": "HOURLY", "captionCode": "jobs.repeat_intervals.HOURLY" },
              { "value": "MINUTELY", "captionCode": "jobs.repeat_intervals.MINUTELY" },
              { "value": "ONDEMAND", "captionCode": "jobs.repeat_intervals.ONDEMAND" }
            ],
            "optionsPerRow": 6,
            "validations": {
              "required": {
                "messageCode": "jobs.repeat_interval_req"
              }
            }
          }
        ]
      },
      {
        "labelCode": "jobs.schedule",
        "fields": [
          {
            "name": "job.scheduledDayOfMonth",
            "labelCode": "jobs.day_of_month",
            "type": "number",
            "validations": {
              "required": {
                "messageCode": "jobs.day_of_month_req"
              },
              "le": {
                "value": 28,
                "messageCode": "jobs.day_of_month_le_28"
              },
              "ge": {
                "value": 1,
                "messageCode": "jobs.day_of_month_ge_1"
              }
            },
            "showWhen": "job.repeatSchedule == 'MONTHLY'"
          },
          {
            "name": "job.scheduledDayOfWeek",
            "labelCode": "jobs.day_of_week",
            "type": "dropdown",
            "listSource": {
              "displayProp": "label",
              "selectProp": "name",
              "options": [
                { "name": "SUNDAY", "label": i18n.msg('jobs.week_days.SUNDAY') },
                { "name": "MONDAY", "label": i18n.msg('jobs.week_days.MONDAY') },
                { "name": "TUESDAY", "label": i18n.msg('jobs.week_days.TUESDAY') },
                { "name": "WEDNESDAY", "label": i18n.msg('jobs.week_days.WEDNESDAY') },
                { "name": "THURSDAY", "label": i18n.msg('jobs.week_days.THURSDAY') },
                { "name": "FRIDAY", "label": i18n.msg('jobs.week_days.FRIDAY') },
                { "name": "SATURDAY", "label": i18n.msg('jobs.week_days.SATURDAY') },
              ]
            },
            "validations": {
              "required": {
                "messageCode": "jobs.day_of_week_req"
              }
            },
            "showWhen": "job.repeatSchedule == 'WEEKLY'"
          },
          {
            "name": "job.scheduledHour",
            "labelCode": "jobs.hour_of_day",
            "type": "number",
            "validations": {
              "required": {
                "messageCode": "jobs.hour_of_day_req"
              },
              "le": {
                "value": 23,
                "messageCode": "jobs.hour_of_day_le_23"
              },
              "ge": {
                "value": 0,
                "messageCode": "jobs.hour_of_day_ge_0"
              }
            },
            "showWhen": "job.repeatSchedule == 'MONTHLY' || job.repeatSchedule == 'WEEKLY' || job.repeatSchedule == 'DAILY'"
          },
          {
            "name": "job.scheduledMinute",
            "labelCode": "jobs.minute_of_hour",
            "type": "number",
            "validations": {
              "required": {
                "messageCode": "jobs.minute_of_hour_req"
              },
              "le": {
                "value": 59,
                "messageCode": "jobs.minute_of_hour_le_59"
              },
              "ge": {
                "value": 0,
                "messageCode": "jobs.minute_of_hour_ge_0"
              }
            },
            "showWhen": "job.repeatSchedule == 'MONTHLY' || job.repeatSchedule == 'WEEKLY' || job.repeatSchedule == 'DAILY' || job.repeatSchedule == 'HOURLY'"
          },
          {
            "name": "job.minutelyInterval",
            "labelCode": "jobs.minutely_interval",
            "type": "number",
            "validations": {
              "required": {
                "messageCode": "jobs.minutely_interval_req"
              },
              "le": {
                "value": 59,
                "messageCode": "jobs.minutely_interval_le_59"
              },
              "ge": {
                "value": 1,
                "messageCode": "jobs.minutely_interval_ge_1"
              }
            },
            "showWhen": "job.repeatSchedule == 'MINUTELY'"
          }
        ]
      }
    ]
  }
}
