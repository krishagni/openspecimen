
import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "container.name",
      "labelCode": "containers.name",
      "type": "span",
      "value": ({container}) => {
        if (container.displayName) {
          return container.displayName + ' (' + container.name + ')';
        }     
                
        return container.name;
      },
      "href": (rowObject) => ui.ngServer + '#/containers/' + rowObject.container.id + '/overview'
    },
    {
      "name": "container.dimension",
      "labelCode": "containers.dimension",
      "type": "span",
      "value": ({container}) => {
        if (container.positionLabelingMode != 'NONE') {
          return container.noOfRows + ' x ' + container.noOfColumns;
        }

        return '-'
      }
    },
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
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.transferredBy",
      "labelCode": "containers.transferred_by",
      "type": "user",
      "validations": {
        "required": {
          "messageCode": "containers.transferred_by_required"
        }
      },
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.transferDate",
      "labelCode": "containers.transfer_date_time",
      "type": "datePicker",
      "showTime": true,
      "validations": {
        "required": {
          "messageCode": "containers.transfer_date_time_required"
        }
      },
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.transferComments",
      "labelCode": "containers.transfer_reasons",
      "type": "textarea",
      "rows": "5",
      "validations": {
        "required": {
          "messageCode": "containers.transfer_reasons_required"
        }
      },
      "enableCopyFirstToAll": true
    }   
  ]
}
