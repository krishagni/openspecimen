export default {
  "layout": {
    "rows": [
      {
        "fields": [ { "name": "type.name" } ]
      },
      {
        "fields": [ { "name": "type.nameFormat" } ]
      },
      {
        "label": "Dimension",

        "fields": [ { "name": "type.noOfRows" }, { "name": "type.noOfColumns" } ]
      },
      {
        "fields": [ { "name": "type.positionLabelingMode" } ]
      },
      {
        "label": "Labeling Scheme",

        "fields": [
          { "label": "Rows",    "name": "type.rowLabelingScheme" },
          { "label": "Columns", "name": "type.columnLabelingScheme" }
        ]
      },
      {
        "fields": [ { "name": "type.positionAssignment" } ]
      },
      {
        "fields": [ { "name": "type.temperature" } ]
      },
      {
        "fields": [ { "name": "type.storeSpecimenEnabled" } ]
      },
      {
        "fields": [ { "name": "type.canHold" } ]
      }
    ]
  }
}
