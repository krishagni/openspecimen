export default {
  fields:  [
    {
      "type": "radio",
      "labelCode": "specimens.lineage",
      "name": "specimen.lineage",
      "options": [
        { "captionCode": "specimens.new", "value": "New" },
        { "captionCode": "specimens.derived", "value": "Derived" },
        { "captionCode": "specimens.aliquot", "value": "Aliquot" }
      ],
      "optionsPerRow": 3,
      "showInOverviewWhen": "true == true",
      "showWhen": "true == false"
    },
    {
      "type": "text",
      "labelCode": "specimens.label",
      "name": "specimen.label",
      "validations": {
        "required": {
          "messageCode": "specimens.label_req"
        }
      },
      "showWhen": "!!specimen.label  || cp.manualSpecLabelEnabled || !specimen.labelFmt",
      "disableWhen": "!cp.manualSpecLabelEnabled && !!specimen.labelFmt"
    },
    {
      "type": "text",
      "labelCode": "specimens.barcode",
      "name": "specimen.barcode",
      "showWhen": "!!specimen.barcode || (cp.barcodingEnabled && !cp.specimenBarcodeFmt)"
    },
    {
      "type": "text",
      "labelCode": "specimens.additional_label",
      "name": "specimen.additionalLabel",
      "showWhen": "!!specimen.additionalLabel || !cp.additionalLabelFmt",
      "disableWhen": "!!cp.additionalLabelFmt"
    },
    {
      "type": "text",
      "labelCode": "specimens.parent_specimen",
      "name": "specimen.parentLabel",
      "showWhen": "true == false",
      "showInOverviewWhen": "specimen.lineage != 'New' && !!specimen.parentLabel"
    },
    {
      "type": "dropdown",
      "labelCode": "specimens.collection_status",
      "name": "specimen.status",
      "listSource": {
        "options": [
          { "name": "Collected" },
          { "name": "Missed Collection" },
          { "name": "Not Collected" },
          { "name": "Pending" }
        ],
        "selectProp": "name",
        "displayProp": "name"
      },
      "validations": {
        "required": {
          "messageCode": "specimens.collection_status_req"
        }
      }
    },
    {
      "type": "specimen-type",
      "labelCode": "specimens.type",
      "name": "specimen.type",
      "entity": "specimen",
      "showWhen": "specimen.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "text",
      "labelCode": "specimens.image_id_url",
      "name": "specimen.imageId",
      "showWhen": "cp.imagingEnabled && specimen.status == 'Collected'"
    },
    {
      "type": "specimen-measure",
      "labelCode": "specimens.initial_quantity",
      "name": "specimen.initialQty",
      "entity": "specimen",
      "measure": "quantity",
      "validations": {
        "requiredIf": {
          "expr": "specimen.lineage == 'Aliquot' && cp.aliquotQtyReq",
          "messageCode": "specimens.initial_quantity_req"
        }
      }
    },
    {
      "type": "specimen-measure",
      "labelCode": "specimens.available_quantity",
      "name": "specimen.availableQty",
      "entity": "specimen",
      "measure": "quantity",
      "validations": {
        "requiredIf": {
          "expr": "specimen.lineage == 'Aliquot' && cp.aliquotQtyReq",
          "messageCode": "specimens.available_quantity_req"
        }
      }
    },
    {
      "type": "specimen-measure",
      "labelCode": "specimens.concentration",
      "name": "specimen.concentration",
      "entity": "specimen",
      "measure": "concentration"
    },
    {
      "type": "datePicker",
      "labelCode": "specimens.created_on",
      "name": "specimen.createdOn",
      "showTime": true,
      "showWhen": "!!specimen.createdOn || specimens.status == 'Collected'"
    },
    {
      "type": "storage-position",
      "labelCode": "specimens.location",
      "name": "specimen.storageLocation",
      "showWhen": "specimen.status == 'Collected'"
    },
    {
      "type": "pv",
      "labelCode": "specimens.biohazards",
      "name": "specimen.biohazards",
      "attribute": "specimen_biohazard",
      "multiple": true,
      "selectProp": "value",
      "showWhen": "specimen.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "pv",
      "labelCode": "specimens.pathology_status",
      "name": "specimen.pathology",
      "attribute": "pathology_status",
      "selectProp": "value",
      "showWhen": "specimen.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "pv",
      "labelCode": "specimens.anatomic_site",
      "name": "specimen.anatomicSite",
      "attribute": "anatomic_site",
      "selectProp": "value",
      "leafValue": true,
      "showWhen": "specimen.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "pv",
      "labelCode": "specimens.laterality",
      "name": "specimen.laterality",
      "attribute": "laterality",
      "selectProp": "value",
      "showWhen": "specimen.lineage != 'Aliquot'",
      "showInOverviewWhen": "true == true"
    },
    {
      "type": "number",
      "labelCode": "specimens.freeze_thaw_cycles",
      "name": "specimen.freezeThawCycles"
    },
    {
      "type": "textarea",
      "labelCode": "specimens.comments",
      "name": "specimen.comments"
    },
    {
      "type": "datePicker",
      "labelCode": "specimens.collection_date",
      "name": "specimen.collectionEvent.time",
      "showTime": true,
      "showWhen": "specimen.lineage == 'New'",
      "showInOverviewWhen": "!!specimen.collectionEvent.time"
    },
    {
      "type": "user",
      "labelCode": "specimens.collector",
      "name": "specimen.collectionEvent.user",
      "showWhen": "specimen.lineage == 'New'",
      "showInOverviewWhen": "!!specimen.collectionEvent.user"
    },
    {
      "type": "pv",
      "labelCode": "specimens.collection_procedure",
      "name": "specimen.collectionEvent.procedure",
      "attribute": "collection_procedure",
      "selectProp": "value",
      "showWhen": "specimen.lineage == 'New'",
      "showInOverviewWhen": "!!specimen.collectionEvent.procedure"
    },
    {
      "type": "pv",
      "labelCode": "specimens.collection_container",
      "name": "specimen.collectionEvent.container",
      "attribute": "collection_container",
      "selectProp": "value",
      "showWhen": "specimen.lineage == 'New'",
      "showInOverviewWhen": "!!specimen.collectionEvent.container"
    },
    {
      "type": "datePicker",
      "labelCode": "specimens.receive_date",
      "name": "specimen.receivedEvent.time",
      "showTime": true,
      "showWhen": "specimen.lineage == 'New'",
      "showInOverviewWhen": "!!specimen.receivedEvent.time"
    },
    {
      "type": "user",
      "labelCode": "specimens.receiver",
      "name": "specimen.receivedEvent.user",
      "showWhen": "specimen.lineage == 'New'",
      "showInOverviewWhen": "!!specimen.receivedEvent.user"
    },
    {
      "type": "pv",
      "labelCode": "specimens.receive_quality",
      "name": "specimen.receivedEvent.receivedQuality",
      "attribute": "receive_quality",
      "selectProp": "value",
      "showWhen": "specimen.lineage == 'New'",
      "showInOverviewWhen": "!!specimen.receivedEvent.receivedQuality"
    }
  ]
}
