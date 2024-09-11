export default {
  key: "cpe.id",

  columns: [
    {
      name: "eventDescription",
      captionCode: "cps.event",
      type: "component",
      component: 'os-visit-event-desc',
      data: ({cpe}) => ({event: cpe})
    },

    {
      name: "cpe.clinicalDiagnosis",
      captionCode: "cps.clinical_diagnosis",
      type: "span"
    },

    {
      name: "cpe.clinicalStatus",
      captionCode: "cps.clinical_status",
      type: "span"
    },

    {
      name: "cpe.defaultSite",
      captionCode: "cps.site",
      type: "span"
    }
  ]
}
