
export default {
  layout: {
    "rows": [
      {
        "fields": [
          {
            "name": "activity.name",
            "label": "Name",
            "type": "text",
            "validations": {
              "required": {
                "message": "Name is mandatory"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.taskName",
            "label": "Task",
            "type": "dropdown",
            "listSource": {
              "apiUrl": "container-tasks",
              "searchProp": "name",
              "selectProp": "name",
              "displayProp": "name"
            },
            "validations": {
              "required": {
                "message": "Task is mandatory"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.startDate",
            "label": "Start Date",
            "type": "datePicker",
            "validations": {
              "required": {
                "message": "Start date is mandatory"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.cycleInterval",
            "label": "Cycle Interval",
            "type": "number",
            "validations": {
              "required": {
                "message": "Cycle interval is mandatory"
              }
            }
          },
          {
            "name": "activity.cycleIntervalUnit",
            "label": "Unit",
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
                "message": "Cycle interval unit is mandatory"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.reminderInterval",
            "label": "Remind Before",
            "type": "number",
            "validations": {
              "required": {
                "message": "Remind before is mandatory"
              }
            }
          },
          {
            "name": "activity.reminderIntervalUnit",
            "label": "Unit",
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
                "message": "Remind before unit is mandatory"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activity.assignedUsers",
            "label": "Assigned Users",
            "type": "user",
            "multiple": true,
            "validations": {
              "required": {
                "message": "Assigned users is mandatory"
              }
            }
          }
        ]
      },
    ]
  }
}
