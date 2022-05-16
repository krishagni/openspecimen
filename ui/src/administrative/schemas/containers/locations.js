export default {
  columns: [
    {
      "name": "autoContainerName",
      "label": "Unique Name",
      "type": "span",
      "value": () => 'Auto',
      "showWhen": "container.typeName"
    },
    {
      "name": "container.name",
      "label": "Unique Name",
      "type": "text",
      "showWhen": "!container.typeName",
      "validations": {
        "required": {
          "message": "Unique Name is mandatory"
        }
      }
    },
    {
      "name": "container.siteName",
      "label": "Site",
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
          "message": "Site is mandatory"
        }
      },
      "enableCopyFirstToAll": true
    },
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
      "uiStyle": {
        "min-width": "300px"
      },
      "enableCopyFirstToAll": true
    }
  ]
}
