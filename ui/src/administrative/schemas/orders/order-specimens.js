
import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "specimen.label",
      "label": "Label",
      "type": "span",
      "href": (rowObject) => ui.ngServer + '#/specimens/' + rowObject.specimen.id,
      "uiStyle": {
        "min-width": "140px"
      },
    },
    {
      "name": "specimen.type",
      "label": "Type",
      "type": "span",
      "uiStyle": {
        "min-width": "140px"
      },
    },
    {
      "name": "specimen.cpShortTitle",
      "label": "Collection Protocol",
      "type": "span",
      "uiStyle": {
        "min-width": "140px"
      },
    },
    {
      "name": "specimen.storageLocation",
      "label": "Location",
      "type": "span",
      "displayType": "storage-position",
      "uiStyle": {
        "min-width": "140px"
      },
    },
    {
      "name": "specimen.availableQty",
      "label": "Available",
      "type": "specimen-measure",
      "entity": "specimen",
      "measure": "quantity",
      "readOnly": true,
      "uiStyle": {
        "min-width": "100px"
      },
    },
    {
      "name": "quantity",
      "label": "Quantity",
      "type": "specimen-measure",
      "entity": "specimen",
      "measure": "quantity",
      "enableCopyFirstToAll": true,
      "uiStyle": {
        "min-width": "100px"
      },
      "validations": {
        "nz": {
          "message": "Distributed quantity cannot be zero"
        },
        "le": {
          "message": "Distributed quantity cannot be greater than the available quantity",
          "expr": "specimen.availableQty",
        },
        "requiredIf": {
          "message": "Distributed quantity is mandatory",
          "expr": "!!specimen.availableQty",
        }
      },
    },
    {
      "name": "holdingLocation",
      "label": "Holding Location",
      "type": "storage-position",
      "showWhen": "allowHoldingLocations",
      "listSource": {
        "queryParams": {
          "static": {
            "entityType": "order_item"
          },
          "dynamic": {
            "dp": "order.distributionProtocol",
          }
        }
      },
      "enableCopyFirstToAll": true,
      "uiStyle": {
        "min-width": "200px"
      },
    },
    {
      "name": "cost",
      "label": "Cost",
      "type": "number",
      "maxFractionDigits": 2,
      "showWhen": "invoicingEnabled",
      "uiStyle": {
        "min-width": "100px"
      },
    },
    {
      "name": "dispose",
      "icon": "ban",
      "type": "booleanCheckbox",
      "tooltip": "Dispose",
      "enableCopyFirstToAll": true,
      "uiStyle": {
        "min-width": "30px"
      }
    },
    {
      "name": "printLabel",
      "icon": "print",
      "type": "booleanCheckbox",
      "tooltip": "Print Labels",
      "enableCopyFirstToAll": true,
      "uiStyle": {
        "min-width": "30px"
      }
    }
  ]
}
