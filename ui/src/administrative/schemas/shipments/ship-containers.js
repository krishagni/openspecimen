
import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "container.name",
      "label": "Name",
      "type": "span",
      "href": (rowObject) => ui.ngServer + '#/containers/' + rowObject.container.id + '/overview'
    },
    {
      "name": "containerDimension",
      "label": "Dimension",
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
      "label": "Specimens",
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
      "label": "Parent Container",
      "type": "span",
      "displayType": "storage-position",
      "showWhen": "!receive"
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
      "label": "Quality",
      "type": "pv",
      "attribute": "shipment_item_received_quality",
      "selectProp": "value",
      "showWhen": "receive || shipment.status == 'Received'",
      "validations": {
        "required": {
          "message": "Received quality is mandatory"
        }
      },
      "uiStyle": {
        "min-width": "175px"
      },
      "enableCopyFirstToAll": true
    }
  ]
}
