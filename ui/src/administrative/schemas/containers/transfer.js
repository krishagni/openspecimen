export default {
  layout: {
    "rows": [
      {
        "fields": [
          {
            "name": "container.siteName",
            "label": "Parent Site",
            "type": "site",
            "listSource": {
              "queryParams": {
                "static": {
                  "resource": "StorageContainer",
                  "operation": "Update"
                }
              },
              "selectProp": "name",
              "displayProp": "name"
            },
            "validations": {
              "required": {
                "message": "Parent Site is mandatory"
              }
            }
          }
        ]
      },

      {
        "fields": [
          {
            "name": "container.storageLocation",
            "label": "Parent Container",
            "type": "storage-position",
            "listSource": {
              "queryParams": {
                "static": {
                  "entityType": "storage_container"
                },
                "dynamic": {
                  "entity": "container",
                  "site": "container.siteName"
                }
              }
            },
            "showWhen": "!!container.siteName"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "container.transferredBy",
            "label": "Transferred By",
            "type": "user",
            "validations": {
              "required": {
                "message": "Transferred By is mandatory"
              }
            }
          }
        ]
      },

      {
        "fields": [
          {
            "name": "container.transferDate",
            "label": "Date and Time",
            "type": "datePicker",
            "showTime": true,
            "validations": {
              "required": {
                "message": "Date and Time is mandatory"
              }
            }
          }
        ]
      },

      {
        "fields": [
          {
            "name": "container.transferComments",
            "label": "Reasons",
            "type": "textarea",
            "rows": "5",
            "validations": {
              "required": {
                "message": "Reasons is mandatory"
              }
            }
          }
        ]
      }
    ]
  }
}
