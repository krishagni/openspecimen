export default {
  fields: [
    {
      "type": "text",
      "name": "sr.name",
      "labelCode": "cps.req_name",
      "showWhen": "sr.lineage == 'New'"
    },
    {
      "type": "text",
      "name": "sr.code",
      "labelCode": "cps.code"
    },
    {
      "type": "specimen-type",
      "name": "sr.type",
      "labelCode": "cps.type",
      "specimen": "sr",
      "entity": "sr",
      "showWhen": "sr.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true",
      "validations": {
        "required": {
          "messageCode": "cps.type_req"
        }
      }
    },
    {
      "type": "pv",
      "attribute": "anatomic_site",
      "selectProp": "value",
      "name": "sr.anatomicSite",
      "labelCode": "cps.anatomic_site",
      "showWhen": "sr.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true",
      "validations": {
        "required": {
          "messageCode": "cps.anatomic_site_req"
        }
      }
    },
    {
      "type": "pv",
      "attribute": "laterality",
      "selectProp": "value",
      "name": "sr.laterality",
      "labelCode": "cps.laterality",
      "showWhen": "sr.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true",
      "validations": {
        "required": {
          "messageCode": "cps.laterality_req"
        }
      }
    },
    {
      "type": "pv",
      "attribute": "pathology_status",
      "selectProp": "value",
      "name": "sr.pathology",
      "labelCode": "cps.pathology_status",
      "showWhen": "sr.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true",
      "validations": {
        "required": {
          "messageCode": "cps.pathology_status_req"
        }
      }
    },
    {
      "type": "specimen-measure",
      "name": "sr.initialQty",
      "labelCode": "cps.quantity",
      "specimen": "sr",
      "entity": "sr",
      "measure": "quantity",
      "showWhen": "sr.id > 0 || sr.lineage != 'Aliquot'"
    },
    {
      "type": "specimen-measure",
      "name": "sr.concentration",
      "labelCode": "cps.concentration",
      "specimen": "sr",
      "entity": "sr",
      "measure": "concentration",
      "showWhen": "sr.lineage != 'Aliquot'",
    },
    {
      "type": "number",
      "maxFractionDigits": "0",
      "name": "sr.sortOrder",
      "labelCode": "cps.sort_order",
      "showWhen": "sr.lineage != 'Aliquot'",
    },
    {
      "type": "radio",
      "name": "sr.storageType",
      "labelCode": "cps.storage_type",
      "options": [
        { "value": "Manual", "captionCode": "common.yes" },
        { "value": "Virtual", "captionCode": "common.no" }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "messageCode": "cps.storage_type_req"
        }
      }
    },
    {
      "type": "user",
      "name": "sr.collector",
      "labelCode": "cps.collector",
      "showWhen": "sr.lineage == 'New'",
      "showInOverviewWhen": "sr.lineage == 'New'"
    },
    {
      "type": "pv",
      "attribute": "collection_container",
      "selectProp": "value",
      "name": "sr.collectionContainer",
      "labelCode": "cps.collection_container",
      "showWhen": "sr.lineage == 'New'",
      "showInOverviewWhen": "sr.lineage == 'New'"
    },
    {
      "type": "pv",
      "attribute": "collection_procedure",
      "selectProp": "value",
      "name": "sr.collectionProcedure",
      "labelCode": "cps.collection_procedure",
      "showWhen": "sr.lineage == 'New'",
      "showInOverviewWhen": "sr.lineage == 'New'"
    },
    {
      "type": "user",
      "name": "sr.receiver",
      "labelCode": "cps.receiver",
      "showWhen": "sr.lineage == 'New'",
      "showInOverviewWhen": "sr.lineage == 'New'"
    },
    {
      "type": "text",
      "name": "sr.labelFmt",
      "labelCode": "cps.label_format"
    },
    {
      "type": "dropdown",
      "name": "sr.labelAutoPrintMode",
      "labelCode": "cps.label_print_mode",
      "listSource": {
        "loadFn": async () => {
           return [
             { label: window.osSvc.i18nSvc.msg('cps.print_modes.PRE_PRINT'),     value: 'PRE_PRINT' },
             { label: window.osSvc.i18nSvc.msg('cps.print_modes.ON_COLLECTION'), value: 'ON_COLLECTION' },
             { label: window.osSvc.i18nSvc.msg('cps.print_modes.NONE'),          value: 'NONE' }
           ]
        },
        "displayProp": "label",
        "selectProp": "value"
      },
      "displayValues": {
        "PRE_PRINT":     () => window.osSvc.i18nSvc.msg("cps.print_modes.PRE_PRINT"),
        "ON_COLLECTION": () => window.osSvc.i18nSvc.msg("cps.print_modes.ON_COLLECTION"),
        "NONE":          () => window.osSvc.i18nSvc.msg("cps.print_modes.NONE")
      }
    },
    {
      "type": "number",
      "maxFractionDigits": 0,
      "name": "sr.labelPrintCopies",
      "labelCode": "cps.label_print_copies"
    },
    {
      "type": "booleanCheckbox",
      "name": "sr.preBarcodedTube",
      "labelCode": "cps.pre_barcoded_tube",
      "showWhen": "barcodingEnabled == true",
      "showInOverviewWhen": "barcodingEnabled == true"
    },
    {
      "type": "textarea",
      "name": "sr.defaultCustomFieldValuesJson",
      "labelCode": "cps.def_custom_field_values"
    }
  ]
}
