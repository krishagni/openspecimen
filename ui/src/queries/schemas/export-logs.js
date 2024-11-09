export default {
  layout: {
    "rows": [
      {
        "labelCode": "queries.report_interval",
        "fields": [ {
          "name": "criteria.startDate",
          "labelCode": "queries.start_date",
          "type": "datePicker"
        }, {
          "name": "criteria.endDate",
          "labelCode": "queries.end_date",
          "type": "datePicker"
        } ]
      },

      {
        "fields": [ {
          "name": "criteria.user",
          "labelCode": "queries.executed_by",
          "type": "user"
        } ]
      }
    ]
  }
}
