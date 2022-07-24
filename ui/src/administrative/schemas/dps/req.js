
export default {
  fields: [
    {
      "type": "pv",
      "name": "requirement.specimenType",
      "labelCode": "dps.specimen_type",
      "attribute": "specimen_type",
      "leafValue": true,
      "selectProp": "value"
    },
    {
      "type": "pv",
      "name": "requirement.anatomicSite",
      "labelCode": "dps.specimen_anatomic_site",
      "attribute": "anatomic_site",
      "leafValue": true,
      "selectProp": "value"
    },
    {
      "type": "pv",
      "name": "requirement.pathologyStatuses",
      "labelCode": "dps.specimen_pathology_status",
      "attribute": "pathology_status",
      "leafValue": true,
      "selectProp": "value",
      "multiple": true
    },
    {
      "type": "pv",
      "name": "requirement.clinicalDiagnosis",
      "labelCode": "dps.visit_clinical_diagnosis",
      "attribute": "clinical_diagnosis",
      "selectProp": "value"
    },
    {
      "type": "number",
      "name": "requirement.specimenCount",
      "labelCode": "dps.specimens_count",
      "maxFractionDigits": 0
    },
    {
      "type": "specimen-measure",
      "name": "requirement.quantity",
      "labelCode": "dps.qty_per_specimen"
    },
    {
      "type": "number",
      "name": "requirement.cost",
      "labelCode": "dps.cost",
      "maxFractionDigits": 2,
      "showWhen": "invoicingEnabled"
    },
    {
      "type": "textarea",
      "name": "requirement.comments",
      "labelCode": "dps.comments"
    }
  ]
}
