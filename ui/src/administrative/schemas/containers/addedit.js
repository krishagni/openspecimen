export default {
  "layout": {
    "rows": [
      {
        "fields": [
          {
            "type": "radio",
            "name": "createType",
            "labelCode": "containers.create_type",
            "options": [
              { "captionCode": "containers.single_container",    "value": "single" },
              { "captionCode": "containers.multiple_containers", "value": "multiple" },
              { "captionCode": "containers.container_hierarchy", "value": "hierarchy" }
            ],
            "optionsPerRow": 3,
            "showWhen": "showCreateType"
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
            }
          }
        ]
      },
      {
        "fields": [
          {
            "type": "number",
            "name": "numOfContainers",
            "labelCode": "containers.num_containers",
            "showWhen": "typeNameFormat && (createType == 'multiple' || createType == 'hierarchy')",
            "validations": {
              "required": {
                "messageCode": "containers.num_containers_required"
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
            "name": "container.displayName",
            "showWhen": "createType == 'single'"
          }
        ]
      },
      {
        "fields": [
          {
            "name": "container.name",
            "showWhen": "!typeNameFormat && createType == 'single'"
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
            "type": "textarea",
            "name": "displayNames",
            "labelCode": "containers.display_names",
            "placeholderCode": "containers.scan_display_names",
            "showWhen": "createType == 'multiple'"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "textarea",
            "name": "uniqueNames",
            "labelCode": "containers.names",
            "placeholderCode": "containers.scan_unique_names",
            "showWhen": "!typeNameFormat && (createType == 'multiple' || createType == 'hierarchy')",
            "validations": {
              "required": {
                "messageCode": "containers.unique_names_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "type": "textarea",
            "name": "barcodes",
            "labelCode": "containers.barcodes",
            "placeholderCode": "containers.scan_barcodes",
            "showWhen": "createType == 'multiple'"
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
              { "captionCode": "common.yes", "value": true },
              { "captionCode": "common.no",  "value": false }
            ],
            "optionsPerRow": 2,
            "showWhen": "!container.id && createType != 'hierarchy'"
          }
        ]
      },
      {
        "labelCode": "containers.dimension",

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
        "labelCode": "containers.labeling_scheme",

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
            "labelCode": "containers.approx_capacity",
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
        "fields": [ { "name": "container.automated" } ]
      },
      {
        "fields": [ { "name": "container.autoFreezerProvider" } ]
      },
      {
        "fields": [
          {
            "name": "container.cellDisplayProp",
            "showWhen": "!container.storageLocation || !container.storageLocation.name"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "multiselect",
            "name": "container.allowedCollectionProtocols",
            "labelCode": "containers.collection_protocols",
            "listSource": {
              "displayProp": "shortTitle",
              "selectProp": "shortTitle",
              "initUsingSelectProp": true,
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
            "labelCode": "containers.specimen_types",
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
            "labelCode": "containers.distribution_protocols",
            "listSource": {
              "displayProp": "shortTitle",
              "selectProp": "shortTitle",
              "initUsingSelectProp": true,
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
            "inlineLabelCode": "containers.print_labels",
            "showWhen": "!container.id"
          }
        ]
      }
    ]
  }
}
