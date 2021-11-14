
export default {
  layout: {
    "rows": [
      {
        "fields": [ { "name": "shipment.name" } ]
      },

      {
        "fields": [ { "name": "shipment.courierName" } ]
      },

      {
        "fields": [ { "name": "shipment.trackingNumber" } ]
      },

      {
        "fields": [ { "name": "shipment.trackingUrl" } ]
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
            "disableWhen": "shipment.status != 'Pending'"
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
        "fields": [ { "name": "shipment.shippedDate" } ]
      },

      {
        "fields": [ { "name": "shipment.senderComments" } ]
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
