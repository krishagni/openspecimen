export default {
  fields: [
    {
      "type": "radio",
      "name": "cp.closeParentSpecimens",
      "labelCode": "cps.close_parent_specimens",
      "options": [
        { "captionCode": "common.yes", "value": true },
        { "captionCode": "common.no",  "value": false }
      ],
      "optionsPerRow": 2
    },
    {
      "type": "radio",
      "name": "cp.setQtyToZero",
      "labelCode": "cps.zero_out_specimen_qty",
      "options": [
        { "captionCode": "common.yes",             "value": true },
        { "captionCode": "common.no",              "value": false },
        { "captionCode": "cps.use_system_setting", "value": 'use_system_setting' }
      ],
      "optionsPerRow": 3,
      "value": ({cp}) => {
        const {osSvc: {i18nSvc: i18n}} = window;
        if (cp.setQtyToZero == true) {
          return i18n.msg("common.yes");
        } else if (cp.setQtyToZero == false) {
          return i18n.msg("common.no");
        } else {
          return i18n.msg("cps.use_system_setting")
        }
      }
    },
    {
      "type": "text",
      "name": "cp.additionalLabelFmt",
      "labelCode": "cps.additional_label_fmt"
    },
    {
      "type": "radio",
      "name": "cp.barcodingEnabled",
      "labelCode": "cps.specimen_barcoding",
      "options": [
        { "captionCode": "common.enabled",  "value": true },
        { "captionCode": "common.disabled", "value": false }
      ],
      "optionsPerRow": 2
    },
    {
      "type": "text",
      "name": "cp.specimenBarcodeFmt",
      "labelCode": "cps.barcode_fmt"
    },
    {
      "type": "radio",
      "name": "cp.labelSequenceKey",
      "labelCode": "cps.label_sequence_key",
      "options": [
        { "captionCode": "cps.record_ids",    "value": "ID" },
        { "captionCode": "cps.record_labels", "value": "LABEL" }
      ],
      "optionsPerRow": 2
    },
    {
      "type": "radio",
      "name": "cp.kitLabelsEnabled",
      "labelCode": "cps.kit_labels",
      "options": [
        { "captionCode": "common.enabled",  "value": true },
        { "captionCode": "common.disabled", "value": false }
      ],
      "optionsPerRow": 2
    },
    {
      "type": "radio",
      "name": "cp.visitCollectionMode",
      "labelCode": "cps.on_visit_collection",
      "options": [
        { "captionCode": "cps.show_primary_specimens", "value": "PRIMARY_SPMNS" },
        { "captionCode": "cps.show_all_specimens",     "value": "ALL_SPMNS" }
      ],
      "optionsPerRow": 2,
      "showWhen": "!cp.specimenCentric",
      "showInOverviewWhen": "!cp.specimenCentric"
    },
    {
      "type": "radio",
      "name": "cp.spmnLabelPrePrintMode",
      "labelCode": "cps.create_pending_specimens",
      "options": [
        { "captionCode": "cps.on_registration",       "value": "ON_REGISTRATION" },
        { "captionCode": "cps.on_visit_collection",   "value": "ON_VISIT" },
        { "captionCode": "cps.on_primary_collection", "value": "ON_PRIMARY_COLL" },
        { "captionCode": "cps.on_primary_receive",    "value": "ON_PRIMARY_RECV" },
        { "captionCode": "cps.on_shipment_receive",   "value": "ON_SHIPMENT_RECV" },
        { "captionCode": "cps.none",                  "value": "NONE" }
      ],
      "optionsPerRow": 3,
      "showWhen": "!cp.specimenCentric",
      "showInOverviewWhen": "!cp.specimenCentric"
    },
    {
      "type": "radio",
      "name": "cp.storageSiteBasedAccess",
      "labelCode": "cps.storage_site_based_access",
      "options": [
        { "captionCode": "common.enabled",  "value": true },
        { "captionCode": "common.disabled", "value": false }
      ],
      "optionsPerRow": 2
    },
    {
      "type": "radio",
      "name": "cp.draftDataEntry",
      "labelCode": "cps.draft_data_entry",
      "options": [
        { "captionCode": "common.enabled",  "value": true },
        { "captionCode": "common.disabled", "value": false }
      ],
      "optionsPerRow": 2
    },
    {
      "type": "subform",
      "name": "cp.spmnLabelPrintSettings",
      "labelCode": "cps.specimen_print_settings",
      "fields": [
        {
          "type": "dropdown",
          "name": "lineage",
          "labelCode": "cps.lineage",
          "listSource": {
            "selectProp": "name",
            "displayProp": "caption",
            "options": [
              { "name": "New",     "caption": window.osSvc.i18nSvc.msg('cps.primary') },
              { "name": "Derived", "caption": window.osSvc.i18nSvc.msg('cps.derived') },
              { "name": "Aliquot", "caption": window.osSvc.i18nSvc.msg('cps.aliquot') }
            ]
          },
        },
        {
          "type": "dropdown",
          "name": "printMode",
          "labelCode": "cps.label_print_mode",
          "listSource": {
            "selectProp": "name",
            "displayProp": "label",
            "options": [
              { "name": "PRE_PRINT",     "label": window.osSvc.i18nSvc.msg("cps.print_modes.PRE_PRINT") },
              { "name": "ON_COLLECTION", "label": window.osSvc.i18nSvc.msg("cps.print_modes.ON_COLLECTION") },
              { "name": "NONE",          "label": window.osSvc.i18nSvc.msg("cps.print_modes.NONE") }
            ]
          }
        },
        {
          "type": "number",
          "name": "copies",
          "labelCode": "cps.copies"
        }
      ],
      "readOnlyCollection": true,
      "disabledFields": ["lineage"]
    }
  ]
}
