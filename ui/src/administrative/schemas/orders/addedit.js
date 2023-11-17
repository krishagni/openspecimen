
export default {
  layout: {
    "rows": [
      {
        "fields": [ { "name": "order.request.id" } ]
      },
      {
        "fields": [ { "name": "order.distributionProtocol" } ]
      },
      {
        "fields": [ { "name": "order.name" } ]
      },
      {
        "fields": [ { "name": "order.instituteName" } ]
      },
      {
        "fields": [ { "name": "order.siteName" } ]
      },
      {
        "fields": [ { "name": "order.requester" } ]
      },
      {
        "fields": [ { "name": "order.executionDate" } ]
      },
      {
        "fields": [ { "name": "order.trackingUrl" } ]
      },
      {
        "fields": [ { "name": "order.checkout", "showWhen": "order.status == 'PENDING'" } ]
      },
      {
        "fields": [ { "name": "order.comments" } ]
      },
      {
        "fields": [ {
          "name": "printLabels",
          "type": "booleanCheckbox",
          "labelCode": "orders.print_labels",
          "showWhen": "mode == 'quick_distribution'"
        } ]
      },
      {
        "fields": [ {
          "name": "closeSpecimens",
          "type": "booleanCheckbox",
          "labelCode": "orders.dispose_specimens",
          "showWhen": "mode == 'quick_distribution'"
        } ]
      }
    ]
  }
}
