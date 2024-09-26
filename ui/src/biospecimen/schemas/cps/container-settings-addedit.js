export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: "dropdown",
            name: "cp.containerSelectionStrategy",
            labelCode: "cps.auto_allocation",
            listSource: {
              selectProp: "name",
              displayProp: "label",
              options: [
                { name: "least-empty",   label: window.osSvc.i18nSvc.msg("cps.auto_allocation_methods.least-empty") },
                { name: "recently-used", label: window.osSvc.i18nSvc.msg("cps.auto_allocation_methods.recently-used") }
              ]
            }
          }
        ]
      },
      {
        fields: [
          {
            type: "booleanCheckbox",
            name: "cp.aliquotsInSameContainer",
            inlineLabelCode: "cps.store_aliquots_in_same_box",
            showWhen: "!!cp.containerSelectionStrategy"
          }
        ]
      }
    ]
  }
}
