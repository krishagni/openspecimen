
export default {
  columns: [
    {
      "name": "specimen.label",
      "labelCode": "orders.specimen.label",
      "type": "span",
      "href": (rowObject) => window.osSvc.routerSvc.getUrl('SpecimenResolver', {specimenId: rowObject.specimen.id}),
      "uiStyle": {
        "min-width": "140px"
      },
    },
    {
      "name": "specimen.type",
      "labelCode": "orders.specimen.type",
      "type": "span",
      "uiStyle": {
        "min-width": "140px"
      },
    },
    {
      "name": "specimen.cpShortTitle",
      "labelCode": "orders.specimen.cp",
      "type": "span",
      "uiStyle": {
        "min-width": "140px"
      },
    },
    {
      "name": "specimen.storageLocation",
      "labelCode": "orders.specimen.location",
      "type": "span",
      "displayType": "storage-position",
      "uiStyle": {
        "min-width": "140px"
      },
    },
    {
      "name": "specimen.availableQty",
      "labelCode": "orders.specimen.available_qty",
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
      "labelCode": "orders.distributed_qty",
      "type": "specimen-measure",
      "entity": "specimen",
      "measure": "quantity",
      "enableCopyFirstToAll": true,
      "uiStyle": {
        "min-width": "100px"
      },
      "validations": {
        "le": {
          "messageCode": "orders.distributed_qty_gt_available_qty",
          "expr": "specimen.availableQty",
        },
        "requiredIf": {
          "messageCode": "orders.distributed_qty_req",
          "expr": "!!specimen.availableQty",
        }
      },
    },
    {
      "name": "holdingLocation",
      "labelCode": "orders.holding_location",
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
      "labelCode": "orders.cost",
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
      "showWhen": "!order.checkout",
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
