export default {
  layout: {
    rows: [
      {
        fields: [ { name: "cp.ppidFmt" } ]
      },
      {
        fields: [ { name: "cp.visitNameFmt" } ]
      },
      {
        fields: [ { name: "cp.specimenLabelFmt" } ]
      },
      {
        fields: [ { name: "cp.derivativeLabelFmt" } ]
      },
      {
        fields: [ { name: "cp.aliquotLabelFmt" } ]
      },
      {
        labelCode: "cps.user_inputs_allowed",
        fields: [
          {
            type: "booleanCheckbox",
            inlineLabelCode: "cps.ppid_full",
            name: "cp.manualPpidEnabled"
          },
          {
            type: "booleanCheckbox",
            inlineLabelCode: "cps.visit_names",
            name: "cp.manualVisitNameEnabled"
          },
          {
            type: "booleanCheckbox",
            inlineLabelCode: "cps.specimen_labels",
            name: "cp.manualSpecLabelEnabled"
          }
        ]
      },
      {
        sectionLabelCode: "cps.misc_settings",
        fields: [ { name: "cp.closeParentSpecimens" } ]
      },
      {
        fields: [ { name: "cp.setQtyToZero", value: null } ]
      },
      {
        fields: [ { name: "cp.additionalLabelFmt" } ]
      },
      {
        fields: [ { name: "cp.barcodingEnabled" } ]
      },
      {
        fields: [ { name: "cp.specimenBarcodeFmt" } ]
      },
      {
        fields: [ { name: "cp.labelSequenceKey" } ]
      },
      {
        fields: [ { name: "cp.visitCollectionMode" } ]
      },
      {
        fields: [ { name: "cp.spmnLabelPrePrintMode" } ]
      },
      {
        fields: [ { name: "cp.storageSiteBasedAccess" } ]
      },
      {
        fields: [ { name: "cp.draftDataEntry" } ]
      },
      {
        fields: [ { name: "cp.spmnLabelPrintSettings" } ]
      }
    ]
  }
}
