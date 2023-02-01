
export default {
  columns: [
    {
      "name": "item.specimenLabel",
      "labelCode": "orders.specimen_title",
      "type": "span",
    },
    {
      "name": "item.orderName",
      "labelCode": "orders.dist_order",
      "type": "span"
    },
    {
      "name": "item.quantity",
      "labelCode": "orders.return_qty",
      "type": "specimen-measure",
      "measure": "quantity",
      "entity": "item.specimen",
      "validations": {
        "le": {
          "messageCode": "orders.ret_qty_gt_dist_qty",
          "expr": "item.distributedQty",
        },
        "requiredIf": {
          "messageCode": "orders.ret_qty_req",
          "expr": "!!item.distributedQty"
        }
      },  
    },
    {
      "name": "item.location",
      "labelCode": "orders.location",
      "type": "storage-position",
      "listSource": {
        "queryParams": {
          "static": {
            "entityType": "specimen"
          },
          "dynamic": {
            "entity": "item.specimen",
          }
        }
      },
      "uiStyle": {
        "min-width": "300px"
      },
    },
    {
      "name": "item.user",
      "labelCode": "orders.user",
      "type": "user"
    },
    {
      "name": "item.time",
      "labelCode": "orders.date_time",
      "type": "datePicker",
      "showTime": true
    },
    {
      "name": "item.incrFreezeThaw",
      "labelCode": "orders.freeze_thaw",
      "type": "number",
      "maxFractionDigits": 0
    },
    {
      "name": "item.comments",
      "labelCode": "orders.comments",
      "type": "textarea"
    }
  ]
}
