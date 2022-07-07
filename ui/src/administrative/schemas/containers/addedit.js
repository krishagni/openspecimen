export default {
  "layout": {
    "rows": [
      {
        "fields": [
          {
            "type": "radio",
            "name": "createType",
            "label": "Create",
            "options": [
              { "caption": "Single Container",    "value": "single" },
              { "caption": "Multiple Containers", "value": "multiple" },
              { "caption": "Container Hierarchy", "value": "hierarchy" }
            ],
            "optionsPerRow": 3,
            "showWhen": "showCreateType && !parentContainer"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "number",
            "name": "numOfContainers",
            "label": "Number of Containers",
            "showWhen": "createType == 'multiple' || createType == 'hierarchy'",
            "validations": {
              "required": {
                "message": "Number of Containers is mandatory"
              }
            }
          }
        ]
      }, 
      {
        "fields": [
          {
            "name": "container.usedFor",
            "showWhen": "!container.id && !parentContainer"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.typeName",
            "validations": {
              "requiredIf": {
                "expr": "createType == 'hierarchy'",
                "message": "Container type is mandatory"
              }
            },
            "showWhen": "dimensionLess != true"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.displayName",
            "showWhen": "createType == 'single'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.name",
            "showWhen": "createType == 'single'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.barcode",
            "showWhen": "createType == 'single'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.siteName",
            "showWhen": "!container.id && !parentContainer"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.storageLocation",
            "showWhen": "!container.id && !parentContainer"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "radio",
            "name": "dimensionLess",
            "label": "Dimensionless?",
            "options": [
              { "caption": "Yes", "value": true },
              { "caption": "No",  "value": false }
            ],
            "optionsPerRow": 2,
            "showWhen": "!container.id && createType != 'hierarchy'"
          }
        ]
      },
      {
        "label": "Dimension",

        "fields": [
          {
            "name": "container.noOfRows",
            "showWhen": "dimensionLess == false"
          },
          {
            "name": "container.noOfColumns",
            "showWhen": "dimensionLess == false"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.positionLabelingMode",
            "showWhen": "dimensionLess == false"
          }
        ]
      },
      {
        "label": "Labeling Scheme",

        "fields": [
          {
            "name": "container.rowLabelingScheme",
            "label": "Rows"
          },
          {
            "name": "container.columnLabelingScheme",
            "label": "Columns"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.positionAssignment",
            "showWhen": "dimensionLess == false"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "number",
            "name": "container.capacity",
            "label": "Approximate Capacity (Specimens)",
            "showWhen": "!container.storageLocation || !container.storageLocation.name"
          }
        ]
      },
      {
        "fields": [ { "name": "container.temperature" } ]
      },
      {
        "fields": [ { "name": "container.storeSpecimensEnabled" } ]
      },
      {
        "fields": [ { "name": "container.cellDisplayProp" } ]
      },
      {
        "fields": [
          {
            "type": "multiselect",
            "name": "container.allowedCollectionProtocols",
            "label": "Collection Protocols",
            "listSource": {
              "displayProp": "shortTitle",
              "selectProp": "shortTitle",
              "loadFn": ({context, query, maxResults}) => context.formData.getAllowedCps({query, maxResults})
            },
            "showWhen": "container.usedFor == 'STORAGE'"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "groupselect",
            "name": "container.allowedTypes",
            "label": "Specimen Types",
            "listSource": {
              "displayProp": "type",
              "groupNameProp": "specimenClass",
              "groupItemsProp": "types",
              "loadFn": ({context}) => context.formData.getAllowedTypes()
            },
            "showWhen": "container.usedFor == 'STORAGE'"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "multiselect",
            "name": "container.allowedDistributionProtocols",
            "label": "Distribution Protocols",
            "listSource": {
              "displayProp": "shortTitle",
              "selectProp": "shortTitle",
              "loadFn": ({context, query, maxResults}) => context.formData.getAllowedDps({query, maxResults})
            },
            "showWhen": "container.usedFor == 'DISTRIBUTION'"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "booleanCheckbox",
            "name": "container.printLabels",
            "inlineLabel": "Print Labels",
            "showWhen": "!container.id"
          }
        ]
      }
    ]
  }
}
