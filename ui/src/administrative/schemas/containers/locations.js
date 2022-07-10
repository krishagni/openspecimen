export default {
  columns: [
    {
      "name": "autoContainerName",
      "labelCode": "containers.unique_name",
      "type": "span",
      "value": () => 'Auto',
      "showWhen": "container.typeName"
    },
    {
      "name": "container.name",
      "labelCode": "containers.unique_name",
      "type": "text",
      "showWhen": "!container.typeName",
      "validations": {
        "required": {
          "messageCode": "containers.unique_name_required"
        }
      }
    },
    {
      "name": "container.siteName",
      "labelCode": "containers.site",
      "type": "site",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name",
        "queryParams": {
          "static": {
            "resource": "StorageContainer",
            "operation": "Update"
          }
        }
      },
      "validations": {
        "required": {
          "messageCode": "containers.site_required"
        }
      },
      "enableCopyFirstToAll": true
    },
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
      "uiStyle": {
        "min-width": "300px"
      },
      "enableCopyFirstToAll": true
    }
  ]
}
