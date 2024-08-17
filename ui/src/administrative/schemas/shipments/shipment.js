export default {
  fields:  [
    {
      "type": "user",
      "labelCode": "shipments.requester",
      "name": "shipment.requester",
      "showWhen": "shipment.request",
      "validations": {
        "required": {
          "messageCode": "shipments.requester_required"
        }
      }
    },
    {
      "type": "pv",
      "labelCode": "shipments.request_status",
      "name": "shipment.requestStatus",
      "showWhen": "shipment.request && shipment.id > 0",
      "attribute": "shipment_request_status",
      "selectProp": "value"
    },
    {
      "type": "textarea",
      "labelCode": "shipments.requester_comments",
      "name": "shipment.requesterComments",
      "showWhen": "shipment.request"
    },
    {
      "type": "text",
      "labelCode": "shipments.name",
      "name": "shipment.name",
      "validations": {
        "required": {
          "messageCode": "shipments.name_required"
        }
      }
    },
    {
      "type": "text",
      "labelCode": "shipments.courier_name",
      "name": "shipment.courierName"
    },
    {
      "type": "text",
      "labelCode": "shipments.tracking_number",
      "name": "shipment.trackingNumber"
    },
    {
      "type": "text",
      "labelCode": "shipments.tracking_url",
      "name": "shipment.trackingUrl"
    },
    {
      "type": "user",
      "labelCode": "shipments.sender",
      "name": "shipment.sender"
    },
    {
      "type": "site",
      "labelCode": "shipments.sending_site",
      "name": "shipment.sendingSite",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name",
        "queryParams": {
          "dynamic": {
            "listAll": "shipment.request"
          },
        }
      },
      "validations": {
        "required": {
          "messageCode": "shipments.sending_site_required"
        }
      }
    },
    {
      "type": "dropdown",
      "labelCode": "shipments.receiving_institute",
      "name": "shipment.receivingInstitute",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name"
      },
      "validations": {
        "required": {
          "messageCode": "shipments.receiving_institute_required"
        }
      }
    },
    {
      "type": "user",
      "labelCode": "shipments.receiver",
      "name": "shipment.receiver"
    },
    {
      "type": "site",
      "labelCode": "shipments.receiving_site",
      "name": "shipment.receivingSite",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name",
        "queryParams": {
          "dynamic": {
            "institute": "shipment.receivingInstitute",
            "listAll": "!shipment.request || shipment.status != 'Pending'"
          }
        }
      },
      "validations": {
        "required": {
          "messageCode": "shipments.receiving_site_required"
        }
      }
    },
    {
      "type": "user",
      "labelCode": "shipments.notify_users",
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
      "labelCode": "shipments.shipped_date",
      "name": "shipment.shippedDate",
      "showTime": true,
      "validations": {
        "required": {
          "messageCode": "shipments.shipped_date_required"
        }
      }
    },
    {
      "type": "textarea",
      "labelCode": "shipments.sender_comments",
      "name": "shipment.senderComments"
    },
    {
      "type": "datePicker",
      "labelCode": "shipments.received_date",
      "name": "shipment.receivedDate",
      "showTime": true,
      "validations": {
        "required": {
          "messageCode": "shipments.received_date_required"
        }
      }
    },
    {
      "type": "textarea",
      "labelCode": "shipments.receiver_comments",
      "name": "shipment.receiverComments"
    }
  ]
}
