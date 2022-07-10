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
        "labelCode": "container_types.dimension",

        "fields": [ { "name": "type.noOfRows" }, { "name": "type.noOfColumns" } ]
      },
      {
        "fields": [ { "name": "type.positionLabelingMode" } ]
      },
      {
        "labelCode": "container_types.labeling_scheme",

        "fields": [
          { "labelCode": "container_types.rows",    "name": "type.rowLabelingScheme" },
          { "labelCode": "container_types.columns", "name": "type.columnLabelingScheme" }
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
