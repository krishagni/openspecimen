
export default {
  columns: [
    {
      "name": "item.specimenLabel",
      "label": "Specimen",
      "type": "span",
    },
    {
      "name": "item.orderName",
      "label": "Distribution Order",
      "type": "span"
    },
    {
      "name": "item.quantity",
      "label": "Return Quantity",
      "type": "specimen-measure",
      "measure": "quantity",
      "entity": "item.specimen",
      "validations": {
        "nz": {
          "message": "Return quantity cannot be zero"
        },
        "le": {
          "message": "Return quantity cannot be greater than the distributed quantity",
          "expr": "item.distributedQty",
        },
        "requiredIf": {
          "message": "Return quantity is mandatory",
          "expr": "!!item.distributedQty"
        }
      },  
    },
    {
      "name": "item.location",
      "label": "Location",
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
      "label": "User",
      "type": "user"
    },
    {
      "name": "item.time",
      "label": "Date and Time",
      "type": "datePicker",
      "showTime": true
    },
    {
      "name": "item.incrFreezeThaw",
      "label": "Freeze/Thaw",
      "type": "number",
      "maxFractionDigits": 0
    },
    {
      "name": "item.comments",
      "label": "Comments",
      "type": "textarea"
    }
  ]
}
