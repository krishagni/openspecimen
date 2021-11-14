export default {
  fields:  [
    {
      "type": "text",
      "label": "Name",
      "name": "shipment.name",
      "validations": {
        "required": {
          "message": "Shipment name is mandatory"
        }
      }
    },
    {
      "type": "text",
      "label": "Courier Name",
      "name": "shipment.courierName"
    },
    {
      "type": "text",
      "label": "Tracking Number",
      "name": "shipment.trackingNumber"
    },
    {
      "type": "text",
      "label": "Tracking URL",
      "name": "shipment.trackingUrl"
    },
    {
      "type": "site",
      "label": "Sending Site",
      "name": "shipment.sendingSite",
      "selectProp": "name",
      "validations": {
        "required": {
          "message": "Sending site is mandatory"
        }
      }
    },
    {
      "type": "dropdown",
      "label": "Receiving Institute",
      "name": "shipment.receivingInstitute",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name"
      },
      "validations": {
        "required": {
          "message": "Receiving institute is mandatory"
        }
      }
    },
    {
      "type": "site",
      "label": "Receiving Site",
      "name": "shipment.receivingSite",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name",
        "queryParams": {
          "dynamic": {
            "institute": "shipment.receivingInstitute"
          }
        }
      },
      "validations": {
        "required": {
          "message": "Receiving site is mandatory"
        }
      }
    },
    {
      "type": "user",
      "label": "Notify Users",
      "name": "shipment.notifyUsers",
      "multiple": true,
      "listSource": {
        "queryParams": {
          "static": {
            "excludeType": ['CONTACT']
          },
          "dynamic": {
            "institute": "shipment.receivingInstitute"
          }
        }
      }
    },
    {
      "type": "datePicker",
      "label": "Shipped Date",
      "name": "shipment.shippedDate",
      "showTime": true,
      "validations": {
        "required": {
          "message": "Shipped date is mandatory"
        }
      }
    },
    {
      "type": "textarea",
      "label": "Sender Comments",
      "name": "shipment.senderComments"
    },
    {
      "type": "datePicker",
      "label": "Received Date",
      "name": "shipment.receivedDate",
      "showTime": true,
      "validations": {
        "required": {
          "message": "Received date is mandatory"
        }
      }
    },
    {
      "type": "textarea",
      "label": "Receiver Comments",
      "name": "shipment.receiverComments"
    }
  ]
}
