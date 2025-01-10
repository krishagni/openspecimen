
export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "parentSr.type",
            type: "specimen-type",
            labelCode: "cps.type",
            entity: "parentSr",
            specimen: "parentSr",
            showWhen: "parentSr && !sr.id && sr.lineage == 'Aliquot'",
            disableWhen: "parentSr && !sr.id && sr.lineage == 'Aliquot'"
          }
        ]
      },
      {
        fields: [
          {
            name: "parentSr.initialQty",
            type: "specimen-measure",
            labelCode: "cps.parent_quantity",
            entity: "parentSr",
            specimen: "parentSr",
            showWhen: "parentSr && !sr.id && sr.lineage == 'Aliquot'",
            disableWhen: "parentSr && !sr.id && sr.lineage == 'Aliquot'"
          }
        ]
      },
      {
        fields: [
          {
            name: "sr.noOfAliquots",
            type: "number",
            maxFractionDigits: 0,
            labelCode: "cps.aliquots_count",
            showWhen: "!sr.id && sr.lineage == 'Aliquot'",
            validations: {
              required: {
                messageCode: "cps.aliquots_count_req"
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            name: "sr.qtyPerAliquot",
            type: "specimen-measure",
            labelCode: "cps.aliquot_qty",
            entity: "parentSr",
            specimen: "parentSr",
            showWhen: "parentSr && !sr.id && sr.lineage == 'Aliquot'",
            validations: {
              required: {
                messageCode: "cps.aliquot_qty_req"
              }
            }
          }
        ]
      },
      {
        fields: [ { name: "sr.name" } ]
      },
      {
        fields: [ { name: "sr.code" } ]
      },
      {
        fields: [ { name: "sr.type" } ]
      },
      {
        fields: [ { name: "sr.anatomicSite" } ]
      },
      {
        fields: [ { name: "sr.laterality" } ]
      },
      {
        fields: [ { name: "sr.pathology" } ]
      },
      {
        fields: [ { name: "sr.initialQty" } ]
      },
      {
        fields: [ { name: "sr.concentration" } ]
      },
      {
        fields: [ { name: "sr.sortOrder" } ]
      },
      {
        fields: [ { name: "sr.storageType" } ]
      },
      {
        fields: [ { name: "sr.collector" } ]
      },
      {
        fields: [ { name: "sr.collectionContainer" } ]
      },
      {
        fields: [ { name: "sr.collectionProcedure" } ]
      },
      {
        fields: [ { name: "sr.receiver" } ]
      },
      {
        fields: [ { name: "sr.labelFmt" } ]
      },
      {
        fields: [ { name: "sr.labelAutoPrintMode" } ]
      },
      {
        fields: [ { name: "sr.labelPrintCopies" } ]
      },
      {
        fields: [ { name: "sr.preBarcodedTube" } ]
      },
      {
        fields: [ { name: "sr.defaultCustomFieldValuesJson" } ]
      }
    ]
  }
}
