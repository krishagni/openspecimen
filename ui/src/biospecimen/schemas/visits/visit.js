export default {
  fields:  [
    {
      "type": "text",
      "labelCode": "visits.name",
      "name": "visit.name",
      "validations": {
        "requiredIf": {
          "expr": "cp.manualVisitNameEnabled",
          "messageCode": "visits.name_req"
        }
      },
      "showWhen": "(!!visit.name || !cp.visitNameFmt || cp.manualVisitNameEnabled) && visit.status != 'Missed Collection' && visit.status != 'Not Collected'",
      "disableWhen": "visit.id > 0 && !!visit.name"
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
      },
      "validations": {
        "required": {
          "messageCode": "visits.status_req"
        }
      }
    },
    {
      "type": "datePicker",
      "labelCode": "visits.visit_date",
      "name": "visit.visitDate",
      "validations": {
        "required": {
          "messageCode": "visits.visit_date_req"
        }
      }
    },
    { 
      "type": "site",
      "labelCode": "visits.site",
      "name": "visit.site",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name"
      },
      "validations": {
        "required": {
          "messageCode": "visits.visit_site_req"
        }
      },
      "showWhen": "visit.status != 'Missed Collection' && visit.status != 'Not Collected'",
    },
    {
      "type": "pv",
      "labelCode": "visits.clinical_diagnoses",
      "name": "visit.clinicalDiagnoses",
      "multiple": true,
      "attribute": "clinical_diagnosis",
      "selectProp": "value",
      "showWhen": "visit.status != 'Missed Collection' && visit.status != 'Not Collected'",
    },
    {
      "type": "pv",
      "labelCode": "visits.clinical_status",
      "name": "visit.clinicalStatus",
      "attribute": "clinical_status",
      "selectProp": "value",
      "showWhen": "visit.status != 'Missed Collection' && visit.status != 'Not Collected'",
    },
    {
      "type": "pv",
      "labelCode": "visits.cohort",
      "name": "visit.cohort",
      "attribute": "cohort",
      "selectProp": "value",
      "showWhen": "visit.status != 'Missed Collection' && visit.status != 'Not Collected'",
    },
    {
      "type": "text",
      "labelCode": "visits.surg_path_number",
      "name": "visit.surgicalPathologyNumber",
      "showWhen": "visit.status != 'Missed Collection' && visit.status != 'Not Collected'",
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
