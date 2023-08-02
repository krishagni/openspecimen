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
        "fields": [
          {
            "type": "radio",
            "name": "dimensionLess",
            "label": "Dimensionless?",
            "options": [
              { "captionCode": "common.yes", "value": true },
              { "captionCode": "common.no",  "value": false }
            ],
            "optionsPerRow": 2,
            "showWhen": "!type.id"
          }
        ]
      },
      {
        "labelCode": "container_types.dimension",

        "fields": [
          {
            "name": "type.noOfRows",
            "showWhen": "dimensionLess != true"
          },
          {
            "name": "type.noOfColumns",
            "showWhen": "dimensionLess != true"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "type.positionLabelingMode",
            "showWhen": "dimensionLess != true"
          }
        ]
      },
      {
        "labelCode": "container_types.labeling_scheme",

        "fields": [
          {
            "labelCode": "container_types.rows",
            "name": "type.rowLabelingScheme",
            "showWhen": "dimensionLess != true"
          },
          {
            "labelCode": "container_types.columns",
            "name": "type.columnLabelingScheme",
            "showWhen": "dimensionLess != true"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "type.positionAssignment",
            "showWhen": "dimensionLess != true"
          }
        ]
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
