
import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "specimenLabel",
      "label": "Label",
      "type": "span",
      "showWhen": "!receive || !allowSpecimenRelabeling",
      "href": (rowObject) => ui.ngServer + '#/specimens/' + rowObject.specimen.id,
      "value": (rowObject) => {
        let spmn = rowObject.specimen;
        let label = spmn.label;
        if (spmn.barcode) {
          label += ' (' + spmn.barcode + ')';
        }

        return label;
      }
    },
    {
      "name": "specimen.label",
      "label": "Label",
      "type": "text",
      "showWhen": "receive && allowSpecimenRelabeling",
      "validations": {
        "required": {
          "message": "Specimen label is mandatory"
        }
      }
    },
    {
      "name": "specimen.externalIds",
      "label": "External ID",
      "type": "span",
      "value": (rowObject) => {
        let externalIds = rowObject.specimen.externalIds;
        if (!externalIds || externalIds.length == 0) {
          return '-';
        }

        return externalIds.map(externalId => externalId.value + ' (' + externalId.name + ')').join(', ');
      },
      "showWhen": (context) => {
        return context.specimenItems.some(item => item.specimen.externalIds && item.specimen.externalIds.length > 0)
      }
    },
    {
      "name": "specimen.type",
      "label": "Type",
      "type": "span"
    },
    {
      "name": "specimen.cpShortTitle",
      "label": "Collection Protocol",
      "type": "span"
    },
    {
      "name": "specimen.ppid",
      "label": "PPID",
      "type": "span",
      "showWhen": (context) => context.specimenItems.some(item => typeof item.specimen.ppid == 'string')
    },
    {
      "name": "specimen.availableQty",
      "label": "Quantity",
      "type": "specimen-measure",
      "entity": "specimen",
      "measure": "quantity",
      "readOnly": "!receive"
    },
    {
      "name": "specimen.storageLocation",
      "label": "Location",
      "type": "span",
      "displayType": "storage-position",
      "showWhen": "!receive"
    },
    {
      "name": "specimen.storageLocation",
      "label": "Location",
      "type": "storage-position",
      "listSource": {
        "queryParams": {
          "static": {
            "entityType": "specimen"
          },
          "dynamic": {
            "entity": "specimen",
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
