export default {
  fields:  [
    {
      "type": "text",
      "labelCode": "visits.name",
      "name": "visit.name"
    },
    { 
      "type": "dropdown",
      "labelCode": "visits.status",
      "name": "visit.status",
      "listSource": {
        "selectProp": "value",
        "displayProp": "value",
        "options": [
          { "value": "Complete" },
          { "value": "Pending" },
          { "value": "Missed Collection" },
          { "value": "Not Collected" }
        ]
      }
    },
    {
      "type": "datePicker",
      "labelCode": "visits.visit_date",
      "name": "visit.visitDate"
    },
    { 
      "type": "site",
      "labelCode": "visits.site",
      "name": "visit.site",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name"
      }
    },
    {
      "type": "pv",
      "labelCode": "visits.clinical_diagnoses",
      "name": "visit.clinicalDiagnoses",
      "multiple": true,
      "attribute": "clinical_diagnosis",
      "selectProp": "value"
    },
    {
      "type": "pv",
      "labelCode": "visits.clinical_status",
      "name": "visit.clinicalStatus",
      "attribute": "clinical_status",
      "selectProp": "value"
    },
    {
      "type": "pv",
      "labelCode": "visits.cohort",
      "name": "visit.cohort",
      "attribute": "cohort",
      "selectProp": "value"
    },
    {
      "type": "text",
      "labelCode": "visits.surg_path_number",
      "name": "visit.surgicalPathologyNumber"
    },
    {
      "type": "user",
      "labelCode": "visits.missed_by",
      "name": "visit.missedBy",
      "showWhen": "visit.status == 'Missed Collection' || visit.status == 'Not Collected'"
    },  
    {
      "type": "pv",
      "labelCode": "visits.missed_reason",
      "name": "visit.missedReason",
      "attribute": "missed_visit_reason",
      "selectProp": "value",
      "showWhen": "visit.status == 'Missed Collection' || visit.status == 'Not Collected'"
    },  
    {
      "type": "textarea",
      "labelCode": "visits.comments",
      "name": "visit.comments"
    } 
  ]
}
