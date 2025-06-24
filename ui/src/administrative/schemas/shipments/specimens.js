import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      "name": "specimen.label",
      "captionCode": "shipments.specimen.label",
      "href": (row) => routerSvc.getUrl('SpecimenResolver', {specimenId: row.rowObject.specimen.id}),
      "value": (rowObject) => {
        let spmn = rowObject.specimen;
        let label = spmn.label;
        if (spmn.barcode) {
          label += ' (' + spmn.barcode + ')';
        }

        return label;
      }
    },
    {
      "name": "specimen.externalIds",
      "captionCode": "shipments.specimen.external_id",
      "value": (rowObject) => {
        let externalIds = rowObject.specimen.externalIds;
        if (!externalIds || externalIds.length == 0) {
          return null;
        }

        return externalIds.map(externalId => externalId.value + ' (' + externalId.name + ')').join(', ');
      },
      "showIf": (context) => context.specimens.some(spmn => spmn.externalIds && spmn.externalIds.length > 0)
    },
    {
      "name": "specimen.type",
      "captionCode": "shipments.specimen.type"
    },
    {
      "name": "specimen.cpShortTitle",
      "captionCode": "shipments.specimen.cp"
    },
    {
      "name": "specimen.ppid",
      "captionCode": "shipments.specimen.ppid",
      "showIf": (context) => context.specimens.some(spmn => typeof spmn.ppid == 'string')
    },
    {
      "name": "specimen.availableQty",
      "captionCode": "shipments.specimen.quantity",
      "type": "specimen-measure",
      "entity": "specimen",
      "measure": "quantity"
    },
    {
      "name": "specimen.storageLocation",
      "captionCode": "shipments.specimen.location",
      "type": "storage-position"
    },
    {
      "name": "receivedQuality",
      "captionCode": "shipments.received_quality"
    }
  ]
}
