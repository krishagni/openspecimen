export default {
  "layout": {
    "rows": [
      {
        "fields": [
          {
            "name": "activityLog.type",
            "labelCode": "containers.type",
            "type": "radio",
            "options": [
              { "captionCode": "containers.activity_type_ad_hoc",    value: "adhoc" },
              { "captionCode": "containers.activity_type_scheduled", value: "scheduled" }
            ],
            "optionsPerRow": 2,
            "validations": {
              "required": {
                "messageCode": "containers.type_required"
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
            "labelCode": "containers.activity",
            "type": "dropdown",
            "listSource": {
              "apiUrl": "container-tasks",
              "searchProp": "name",
              "selectProp": "name",
              "displayProp": "name"
            },
            "validations": {
              "required": {
                "messageCode": "containers.activity_required"
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
            "labelCode": "containers.activity",
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
                "messageCode": "containers.activity_required"
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
            "labelCode": "containers.activity",
            "type": "span",
            "showWhen": "!!activityLog.id"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.performedBy",
            "labelCode": "containers.performed_by",
            "type": "user",
            "validations": {
              "required": {
                "messageCode": "containers.performed_by_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.activityDate",
            "labelCode": "containers.activity_date",
            "type": "datePicker",
            "validations": {
              "required": {
                "messageCode": "containers.activity_date_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.timeTaken",
            "labelCode": "containers.activity_time_taken",
            "type": "number"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "activityLog.comments",
            "labelCode": "common.comments",
            "type": "textarea",
            "rows": "3"
          }
        ]
      }
    ]
  }
}
