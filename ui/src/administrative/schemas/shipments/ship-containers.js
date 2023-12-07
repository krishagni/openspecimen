
import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "container.name",
      "labelCode": "shipments.container_name",
      "type": "span",
      "href": ({container}) => ui.ngServer + '#/containers/' + container.id + '/overview'
    },
    {
      "name": "containerDimension",
      "labelCode": "shipments.container_dimension",
      "type": "span",
      "value": ({container}) => {
        if (container.positionLabelingMode != 'NONE') {
          return container.noOfRows + ' x ' + container.noOfColumns;
        }

        return '-'
      }
    },
    {
      "name": "allowedCps",
      "labelCode": "shipments.container_cps",
      "type": "span",
      "value": ({container}) => {
        if (container.usedFor == 'STORAGE') {
          if (container.calcAllowedCollectionProtocols.length > 0) {
            return container.calcAllowedCollectionProtocols.join(', ');
          } else {
            return 'All';
          }
        }

        return '-';
      }
    },
    {
      "name": "allowedTypes",
      "labelCode": "shipments.container_specimen_types",
      "type": "span",
      "value": ({container}) => {
        if (container.usedFor == 'STORAGE') {
          let types = [];
          if (container.calcAllowedSpecimenClasses.length > 0) {
            Array.prototype.push.apply(types, container.calcAllowedSpecimenClasses);
          }

          if (container.calcAllowedSpecimenTypes.length > 0) {
            Array.prototype.push.apply(types, container.calcAllowedSpecimenTypes);
          }

          if (types.length > 0) {
            return types.join(', ');
          }
        }

        return '-';
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
