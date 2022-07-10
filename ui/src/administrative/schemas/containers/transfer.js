export default {
  layout: {
    "rows": [
      {
        "fields": [
          {
            "name": "container.siteName",
            "labelCode": "containers.site",
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
                "messageCode": "containers.site_required"
              }
            }
          }
        ]
      },

      {
        "fields": [
          {
            "name": "container.storageLocation",
            "labelCode": "containers.parent_container",
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
            "labelCode": "containers.transferred_by",
            "type": "user",
            "validations": {
              "required": {
                "messageCode": "containers.transferred_by_required"
              }
            }
          }
        ]
      },

      {
        "fields": [
          {
            "name": "container.transferDate",
            "labelCode": "containers.transfer_date_time",
            "type": "datePicker",
            "showTime": true,
            "validations": {
              "required": {
                "messageCode": "containers.transfer_date_time_required"
              }
            }
          }
        ]
      },

      {
        "fields": [
          {
            "name": "container.transferComments",
            "labelCode": "containers.transfer_reasons",
            "type": "textarea",
            "rows": "5",
            "validations": {
              "required": {
                "messageCode": "containers.transfer_reasons_required"
              }
            }
          }
        ]
      }
    ]
  }
}
