
export default {
  fields: [
    {
      "type": "pv",
      "name": "requirement.specimenType",
      "label": "Specimen Type",
      "attribute": "specimen_type",
      "leafValue": true,
      "selectProp": "value"
    },
    {
      "type": "pv",
      "name": "requirement.anatomicSite",
      "label": "Anatomic Site",
      "attribute": "anatomic_site",
      "leafValue": true,
      "selectProp": "value"
    },
    {
      "type": "pv",
      "name": "requirement.pathologyStatuses",
      "label": "Pathology Status",
      "attribute": "pathology_status",
      "leafValue": true,
      "selectProp": "value",
      "multiple": true
    },
    {
      "type": "pv",
      "name": "requirement.clinicalDiagnosis",
      "label": "Clinical Diagnosis",
      "attribute": "clinical_diagnosis",
      "selectProp": "value"
    },
    {
      "type": "number",
      "name": "requirement.specimenCount",
      "label": "Specimen Count",
      "maxFractionDigits": 0
    },
    {
      "type": "specimen-measure",
      "name": "requirement.quantity",
      "label": "Quantity Per Specimen"
    },
    {
      "type": "number",
      "name": "requirement.cost",
      "label": "Cost",
      "maxFractionDigits": 2,
      "showWhen": "invoicingEnabled"
    },
    {
      "type": "textarea",
      "name": "requirement.comments",
      "label": "Comments"
    }
  ]
}
