
import routerSvc from '@/common/services/Router.js';

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
      "uiStyle": {
        "min-width": "125px"
      },
      "href": ({container}) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: container.id})
    },
    {
      "name": "container.barcode",
      "labelCode": "containers.barcode",
      "type": "span",
      "href": ({container}) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: container.id})
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
      },
      "uiStyle": {
        "min-width": "75px"
      },
      showWhen: "!findPlaces"
    },
    {
      "name": "container.typeName",
      "labelCode": "containers.type",
      "type": "span",
      "uiStyle": {
        "min-width": "75px"
      },
      "showWhen": "findPlaces"
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
      "uiStyle": {
        "min-width": "200px"
      },
      "showWhen": "!checkout && !findPlaces",
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.siteName",
      "labelCode": "containers.site",
      "type": "span",
      "showWhen": "checkout"
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
      "showWhen": "!checkout && !findPlaces",
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.storageLocation.name",
      "labelCode": "containers.transfer_to",
      "type": "span",
      "uiStyle": {
        "min-width": "150px"
      },
      "showWhen": "findPlaces"
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
      "uiStyle": {
        "min-width": "200px"
      },
      "showWhen": "!checkin && !checkout",
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.transferredBy",
      "labelCode": "containers.checked_out_by",
      "type": "user",
      "validations": {
        "required": {
          "messageCode": "containers.checked_out_by_required"
        }
      },
      "uiStyle": {
        "min-width": "200px"
      },
      "showWhen": "checkout",
      "enableCopyFirstToAll": true
    },
    {
      "name": "container.transferredBy",
      "labelCode": "containers.checked_in_by",
      "type": "user",
      "validations": {
        "required": {
          "messageCode": "containers.checked_in_by_required"
        }
      },
      "uiStyle": {
        "min-width": "200px"
      },
      "showWhen": "checkin",
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
