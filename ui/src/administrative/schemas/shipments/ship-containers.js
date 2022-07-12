
import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "container.name",
      "labelCode": "shipments.container_name",
      "type": "span",
      "href": (rowObject) => ui.ngServer + '#/containers/' + rowObject.container.id + '/overview'
    },
    {
      "name": "containerDimension",
      "labelCode": "shipments.container_dimension",
      "type": "span",
      "value": (rowObject) => {
        let container = rowObject.container;
        if (container.positionLabelingMode != 'NONE') {
          return container.noOfRows + ' x ' + container.noOfColumns;
        }

        return '-'
      }
    },
    {
      "name": "storedSpecimens",
      "labelCode": "shipments.specimens",
      "type": "span",
      "value": (rowObject) => {
        if (rowObject.container.storedSpecimens != null) {
          return rowObject.container.storedSpecimens;
        } else if (rowObject.specimensCount != null) {
          return rowObject.specimensCount;
        }

        return '-';
      }
    },
    {
      "name": "container.storageLocation",
      "labelCode": "shipments.container_parent",
      "type": "span",
      "displayType": "storage-position",
      "showWhen": "!receive"
    },
    {
      "name": "container.storageLocation",
      "labelCode": "shipments.container_parent",
      "type": "storage-position",
      "listSource": {
        "queryParams": {
          "static": {
            "entityType": "storage_container"
          },
          "dynamic": {
            "entity": "container",
            "site": "shipment.receivingSite"
          }
        }
      },
      "showWhen": "receive",
      "uiStyle": {
        "min-width": "300px"
      },
      "enableCopyFirstToAll": true
    },
    {
      "name": "receivedQuality",
      "labelCode": "shipments.received_quality",
      "type": "pv",
      "attribute": "shipment_item_received_quality",
      "selectProp": "value",
      "showWhen": "receive || shipment.status == 'Received'",
      "validations": {
        "required": {
          "messageCode": "shipments.received_quality_required"
        }
      },
      "uiStyle": {
        "min-width": "175px"
      },
      "enableCopyFirstToAll": true
    }
  ]
}
