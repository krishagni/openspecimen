export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: "text",
            labelCode: "cps.code",
            name: "cpe.code"
          }
        ]
      },
      {
        labelCode: "cps.event_point",
        fields: [
          {
            type: "number",
            labelCode: "cps.interval",
            name: "cpe.eventPoint",
            maxFractionDigits: 0
          },
          {
            type: "dropdown",
            labelCode: "cps.unit",
            name: "cpe.eventPointUnit",
            listSource: {
              selectProp: "value",
              displayProp: "label",
              loadFn: ({context}) => context.formData.getIntervalUnits()
            }
          }
        ]
      },
      {
        fields: [
          {
            type: "text",
            labelCode: "cps.event_label",
            name: "cpe.eventLabel",
            validations: {
              required: {
                messageCode: "cps.event_label_req"
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: "pv",
            attribute: "clinical_diagnosis",
            labelCode: "cps.clinical_diagnosis",
            selectProp: "value",
            name: "cpe.clinicalDiagnosis",
            validations: {
              required: {
                messageCode: "cps.clinical_diagnosis_req"
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: "pv",
            attribute: "clinical_status",
            selectProp: "value",
            labelCode: "cps.clinical_status",
            name: "cpe.clinicalStatus",
            validations: {
              required: {
                messageCode: "cps.cps.clinical_status_req"
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: "site",
            labelCode: "cps.site",
            name: "cpe.defaultSite",
            selectProp: "name"
          }
        ]
      }
    ]
  }
}
