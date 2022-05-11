export default {
  "layout": {
    "rows": [
      {
        "fields": [
          {
            "name": "activityLog.type",
            "label": "Type",
            "type": "radio",
            "options": [
              { "caption": "Ad-hoc", value: "adhoc" },
              { "caption": "Scheduled", value: "scheduled" }
            ],
            "optionsPerRow": 2,
            "validations": {
              "required": {
                "message": "Type is mandatory"
              }
            },
            "showWhen": "!activityLog.id"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.taskName",
            "label": "Activity",
            "type": "dropdown",
            "listSource": {
              "apiUrl": "container-tasks",
              "searchProp": "name",
              "selectProp": "name",
              "displayProp": "name"
            },
            "validations": {
              "required": {
                "message": "Activity is mandatory"
              }
            },
            "showWhen": "!activityLog.id && activityLog.type == 'adhoc'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.scheduledActivityId",
            "label": "Activity",
            "type": "dropdown",
            "listSource": {
              "apiUrl": "scheduled-container-activities",
              "queryParams": {
                "dynamic": {
                  "containerId": "activityLog.containerId"
                }
              },
              "searchProp": "name",
              "selectProp": "id",
              "displayProp": "name"
            },
            "validations": {
              "required": {
                "message": "Activity is mandatory"
              }
            },
            "showWhen": "!activityLog.id && activityLog.type == 'scheduled'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.activity",
            "label": "Activity",
            "type": "span",
            "showWhen": "!!activityLog.id"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.performedBy",
            "label": "Performed By",
            "type": "user",
            "validations": {
              "required": {
                "message": "Performed By is mandatory"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.activityDate",
            "label": "Activity Date",
            "type": "datePicker",
            "validations": {
              "required": {
                "message": "Activity date is mandatory"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.timeTaken",
            "label": "Time Taken (mins)",
            "type": "number"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.comments",
            "label": "Comments",
            "type": "textarea",
            "rows": "3"
          }
        ]
      }
    ]
  }
}
