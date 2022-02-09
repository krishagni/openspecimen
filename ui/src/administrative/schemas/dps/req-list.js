export default {
  columns: [
    {
      "name": "requirement.specimenType",
      "caption": "Specimen Type",
    },
    {
      "name": "requirement.anatomicSite",
      "caption": "Anatomic Site",
    },
    {
      "name": "requirement.pathologyStatuses",
      "caption": "Pathology Status",
    },
    {
      "name": "requirement.clinicalDiagnosis",
      "caption": "Clinical Diagnosis",
    },
    {
      "name": "requirement.cost",
      "caption": "Cost",
    },
    {
      "name": "requirement.target",
      "caption": "Target Quantity",
      "value": function({requirement}) {
        if (requirement.specimenCount != null && requirement.specimenCount != undefined &&
            requirement.quantity != null && requirement.quantity != undefined) {
          return requirement.specimenCount * requirement.quantity;
        }

        return '-';
      }
    },
    {
      "name": "requirement.distributedCnt",
      "caption": "Distributed Specimens"
    },
    {
      "name": "requirement.distributedQty",
      "caption": "Distributed Quantity"
    }
  ]
}
