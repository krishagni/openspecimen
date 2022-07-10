export default {
  "layout": {
    "rows": [
      {
        "fields": [
          {
            "name": "activity.name",
            "labelCode": "containers.name",
            "type": "text",
            "validations": {
              "required": {
                "messageCode": "containers.name_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.taskName",
            "labelCode": "containers.task",
            "type": "dropdown",
            "listSource": {
              "apiUrl": "container-tasks",
              "searchProp": "name",
              "selectProp": "name",
              "displayProp": "name"
            },
            "validations": {
              "required": {
                "messageCode": "containers.task_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.startDate",
            "labelCode": "containers.start_date",
            "type": "datePicker",
            "validations": {
              "required": {
                "messageCode": "containers.start_date_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.cycleInterval",
            "labelCode": "containers.cycle_interval",
            "type": "number",
            "validations": {
              "required": {
                "messageCode": "containers.cycle_interval_required"
              }
            }
          },
          {
            "name": "activity.cycleIntervalUnit",
            "labelCode": "containers.interval_unit",
            "type": "dropdown",
            "listSource": {
              "options": [
                { "name": "DAYS",   "label": "Days" },
                { "name": "WEEKS",  "label": "Weeks" },
                { "name": "MONTHS", "label": "Months" },
                { "name": "YEARS",  "label": "Years" }
              ],
              "selectProp": "name",
              "displayProp": "label"
            },
            "validations": {
              "required": {
                "messageCode": "containers.cycle_interval_unit_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.reminderInterval",
            "labelCode": "containers.remind_before",
            "type": "number",
            "validations": {
              "required": {
                "messageCode": "containers.remind_before_required"
              }
            }
          },
          {
            "name": "activity.reminderIntervalUnit",
            "labelCode": "containers.interval_unit",
            "type": "dropdown",
            "listSource": {
              "options": [
                { "name": "DAYS",   "label": "Days" },
                { "name": "WEEKS",  "label": "Weeks" },
                { "name": "MONTHS", "label": "Months" },
                { "name": "YEARS",  "label": "Years" }
              ],
              "selectProp": "name",
              "displayProp": "label"
            },
            "validations": {
              "required": {
                "messageCode": "containers.remind_before_interval_unit_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.assignedUsers",
            "labelCode": "containers.assigned_users",
            "type": "user",
            "multiple": true,
            "validations": {
              "required": {
                "messageCode": "containers.assigned_users_required"
              }
            }
          }
        ]
      },
    ]
  }
}
