
export default {
  layout: {
    "rows": [
      {
        "fields": [
          {
            "name": "shipment.request",
            "type": "booleanCheckbox",
            "inlineLabelCode": "shipments.shipment_request",
            "disableWhen": "shipment.id > 0"
          }
        ]
      },

      {
        "fields": [ { "name": "shipment.requestStatus" } ]
      },

      {
        "fields": [ { "name": "shipment.name" } ]
      },

      {
        "fields": [
          {
            "name": "shipment.courierName",
            "showWhen": "!shipment.request || ship || receive || (shipment.status != 'Pending' && shipment.status != 'Requested')"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.trackingNumber",
            "showWhen": "!shipment.request || ship || receive || (shipment.status != 'Pending' && shipment.status != 'Requested')"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.trackingUrl",
            "showWhen": "!shipment.request || ship || receive || (shipment.status != 'Pending' && shipment.status != 'Requested')"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.sendingSite",
            "disableWhen": "shipment.status != 'Pending'"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.receivingInstitute",
            "disableWhen": "shipment.status != 'Pending'",
            "showWhen": "currentUser.admin || !shipment.request"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.receivingSite",
            "disableWhen": "shipment.status != 'Pending'"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.notifyUsers",
            "disableWhen": "shipment.status != 'Pending'"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.requesterComments",
            "showWhen": "shipment.request"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.shippedDate",
            "showWhen": "!shipment.request || ship || receive || (shipment.status != 'Pending' && shipment.status != 'Requested')"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.senderComments",
            "showWhen": "!shipment.request || ship || receive || (shipment.status != 'Pending' && shipment.status != 'Requested')"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.receivedDate",
            "showWhen": "receive || shipment.status == 'Received'"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "shipment.receiverComments",
            "showWhen": "receive || shipment.status == 'Received'"
          }
        ]
      }
    ]
  }
}
