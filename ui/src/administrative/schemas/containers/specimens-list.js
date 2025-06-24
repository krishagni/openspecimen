import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      "captionCode": "containers.specimen.cp",
      "name": "specimen.cpShortTitle",
      "type": "span",
      "href": ({rowObject}) => {
        const specimen = rowObject.specimen || {};
        return routerSvc.getUrl('CpDetail.Overview', {cpId: specimen.cpId});
      }
    },
    {
      "captionCode": "containers.specimen.label",
      "name": "specimen.label",
      "type": "span",
      "href": ({rowObject}) => {
        const specimen = rowObject.specimen || {};
        return routerSvc.getUrl('SpecimenResolver', {specimenId: specimen.id});
      },
      "value": function({specimen}) {
        let label = specimen.label;
        if (specimen.barcode) {
          label += ' (' + specimen.barcode + ')';
        }

        return label;
      }
    },
    { 
      "captionCode": "containers.specimen.type",
      "name": "specimen.type", 
      "type": "span",
      "uiStyle": {
        "min-width": "140px"
      }
    },
    {
      "captionCode": "containers.specimen.available_qty",
      "name": "specimen.availableQty",
      "type": "specimen-measure",
      "entity": "specimen",
      "measure": "quantity",
      "readOnly": true,
      "uiStyle": {
        "min-width": "100px"
      }
    },
    {
      "captionCode": "containers.specimen.checkout_location",
      "name": "specimen.checkoutPosition",
      "type": "storage-position",
      "displayType": "storage-position",
      "uiStyle": {
        "min-width": "140px"
      }
    }
  ]
}
