export default {
  fields: [
    {
      "type": "text",
      "name": "cp.ppidFmt",
      "labelCode": "cps.ppid",
      "showWhen": "!cp.specimenCentric",
      "showInOverviewWhen": "!cp.specimenCentric"
    },
    {
      "type": "text",
      "name": "cp.visitNameFmt",
      "labelCode": "cps.visit",
      "showWhen": "!cp.specimenCentric",
      "showInOverviewWhen": "!cp.specimenCentric"
    },
    {
      "type": "text",
      "name": "cp.specimenLabelFmt",
      "labelCode": "cps.primary_specimen"
    },
    {
      "type": "text",
      "name": "cp.derivativeLabelFmt",
      "labelCode": "cps.derived_specimen"
    },
    {
      "type": "text",
      "name": "cp.aliquotLabelFmt",
      "labelCode": "cps.aliquot"
    },
    {
      "type": "text",
      "name": "calcManualInput",
      "labelCode": "cps.user_inputs_allowed",
      "value": ({cp}) => {
        const {osSvc: {i18nSvc: i18n}} = window;
        const result = [];
        if (cp.manualPpidEnabled) {
          result.push(i18n.msg('cps.ppid'));
        }

        if (cp.manualVisitNameEnabled) {
          result.push(i18n.msg('cps.visit'));
        }

        if (cp.manualSpecLabelEnabled) {
          result.push(i18n.msg('cps.specimen'));
        }

        return result.join(', ');
      }
    }
  ]
}
