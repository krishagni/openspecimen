
export default {
  fields: [
    {
      "type": "text",
      "name": "sr.name",
      "labelCode": "cps.req_name"
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
      "showWhen": "sr.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "pv",
      "attribute": "anatomic_site",
      "selectProp": "value",
      "name": "sr.anatomicSite",
      "labelCode": "cps.anatomic_site",
      "showWhen": "sr.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "pv",
      "attribute": "laterality",
      "selectProp": "value",
      "name": "sr.laterality",
      "labelCode": "cps.laterality",
      "showWhen": "sr.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "pv",
      "attribute": "pathology_status",
      "selectProp": "value",
      "name": "sr.pathology",
      "labelCode": "cps.pathology_status",
      "showWhen": "sr.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "specimen-measure",
      "name": "sr.initialQty",
      "labelCode": "cps.quantity",
      "specimen": "sr",
      "measure": "quantity"
    },
    {
      "type": "specimen-measure",
      "name": "sr.concentration",
      "labelCode": "cps.concentration",
      "specimen": "sr",
      "measure": "concentration"
    },
    {
      "type": "number",
      "maxFractionDigits": "0",
      "name": "sr.sortOrder",
      "labelCode": "cps.sort_order"
    },
    {
      "type": "radio",
      "name": "sr.storageType",
      "labelCode": "cps.storage_type",
      "options": [
        { "value": "Manual", "captionCode": "common.yes" },
        { "value": "Virtual", "captionCode": "common.no" }
      ],
      "optionsPerRow": 2
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
      "name": "sr.labelFormat",
      "labelCode": "cps.label_format"
    },
    {
      "type": "dropdown",
      "name": "sr.labelAutoPrintMode",
      "labelCode": "cps.label_print_mode",
      "listSource": {
        "loadFn": ({context}) => context.formData.getPrintModes(),
        "displayProp": "label",
        "selectProp": "value"
      }
    },
    {
      "type": "number",
      "maxFractionDigits": 0,
      "name": "sr.labelPrintCopies",
      "labelCode": "cps.label_print_copies"
    },
    {
      "type": "textarea",
      "name": "sr.defaultCustomFieldValues",
      "labelCode": "cps.def_custom_field_values",
      "value": ({sr}) => sr.defaultCustomFieldValues ? JSON.stringify(sr.defaultCustomFieldValues, null, 2) : null
    }
  ]
}
