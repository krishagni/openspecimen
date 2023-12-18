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
      ]
    },
    {
      "type": "text",
      "labelCode": "specimens.label",
      "name": "specimen.label"
    },
    {
      "type": "text",
      "labelCode": "specimens.barcode",
      "name": "specimen.barcode"
    },
    {
      "type": "text",
      "labelCode": "specimens.additional_label",
      "name": "specimen.additionalLabel"
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
      }
    },
    {
      "type": "specimen-type",
      "labelCode": "specimens.type",
      "name": "specimen.type"
    },
    {
      "type": "text",
      "labelCode": "specimens.image_id_url",
      "name": "specimen.imageId"
    },
    {
      "type": "specimen-quantity",
      "labelCode": "specimens.initial_quantity",
      "name": "specimen.initialQty",
      "specimen": "specimen"
    },
    {
      "type": "specimen-quantity",
      "labelCode": "specimens.available_quantity",
      "name": "specimen.availableQty",
      "specimen": "specimen"
    },
    {
      "type": "specimen-measure",
      "labelCode": "specimens.concentration",
      "name": "specimen.concentration",
      "specimen": "specimen",
      "measure": "concentration"
    },
    {
      "type": "datetime",
      "labelCode": "specimens.created_on",
      "name": "specimen.createdOn"
    },
    {
      "type": "storage-position",
      "labelCode": "specimens.location",
      "name": "specimen.storageLocation"
    },
    {
      "type": "pv",
      "labelCode": "specimens.biohazards",
      "name": "specimen.biohazards",
      "attribute": "specimen_biohazard",
      "multiple": true
    },
    {
      "type": "pv",
      "labelCode": "specimens.pathology_status",
      "name": "specimen.pathology",
      "attribute": "pathology_status"
    },
    {
      "type": "pv",
      "labelCode": "specimens.anatomic_site",
      "name": "specimen.anatomicSite",
      "attribute": "anatomic_site"
    },
    {
      "type": "pv",
      "labelCode": "specimens.laterality",
      "name": "specimen.laterality",
      "attribute": "laterality"
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
    }
  ]
}
