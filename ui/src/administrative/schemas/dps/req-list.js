export default {
  columns: [
    {
      "name": "requirement.specimenType",
      "captionCode": "dps.specimen_type",
    },
    {
      "name": "requirement.anatomicSite",
      "captionCode": "dps.specimen_anatomic_site",
    },
    {
      "name": "requirement.pathologyStatuses",
      "captionCode": "dps.specimen_pathology_status"
    },
    {
      "name": "requirement.clinicalDiagnosis",
      "captionCode": "dps.visit_clinical_diagnosis"
    },
    {
      "name": "requirement.cost",
      "captionCode": "dps.cost",
    },
    {
      "name": "requirement.target",
      "captionCode": "dps.target_quantity",
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
      "captionCode": "dps.distributed_specimens"
    },
    {
      "name": "requirement.distributedQty",
      "captionCode": "dps.distributed_qty"
    }
  ]
}
