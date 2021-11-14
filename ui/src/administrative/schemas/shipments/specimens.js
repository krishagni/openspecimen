import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "specimen.label",
      "caption": "Label",
      "href": (row) => ui.ngServer + '#/specimens/' + row.rowObject.specimen.id,
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
      "caption": "External ID",
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
      "caption": "Type"
    },
    {
      "name": "specimen.cpShortTitle",
      "caption": "Collection Protocol"
    },
    {
      "name": "specimen.ppid",
      "caption": "PPID",
      "showIf": (context) => context.specimens.some(spmn => typeof spmn.ppid == 'string')
    },
    {
      "name": "specimen.availableQty",
      "caption": "Quantity",
      "type": "specimen-measure",
      "entity": "specimen",
      "measure": "quantity"
    },
    {
      "name": "specimen.storageLocation",
      "caption": "Location",
      "type": "storage-position"
    },
    {
      "name": "receivedQuality",
      "caption": "Quality"
    }
  ]
}
