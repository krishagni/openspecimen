import routerSvc from '@/common/services/Router.js';

export default {
  fields:  [
    {
      "type": "text",
      "label": "Collection Protocol",
      "name": "specimen.cpShortTitle",
      "href": (data) => {
        const specimen = data.specimen || {};
        return routerSvc.ngUrl('cps/' + specimen.cpId + '/overview');
      }
    },
    {
      "type": "text",
      "label": "Label",
      "name": "specimen.label",
      "href": (data) => {
        const specimen = data.specimen || {};
        return routerSvc.ngUrl('specimens/' + specimen.id);
      }
    },
    {
      "type": "text",
      "label": "Barcode",
      "name": "specimen.barcode",
      "href": (data) => {
        const specimen = data.specimen || {};
        return routerSvc.ngUrl('specimens/' + specimen.id);
      }
    },
    {
      "type": "text",
      "label": "Parent Specimen",
      "name": "specimen.parentLabel",
      "href": (data) => {
        const specimen = data.specimen || {};
        return specimen.parentId > 0 ? routerSvc.ngUrl('specimens/' + specimen.parentId) : null;
      }
    },
    {
      "type": "text",
      "label": "PPID",
      "name": "specimen.ppid",
      "href": (data) => {
        const specimen = data.specimen || {};
        const {cpId, cprId} = specimen;
        return cprId > 0 ? routerSvc.ngUrl('cp-view/' + cpId + '/participants/' + cprId + '/detail/overview') : null;
      }
    },
    {
      "type": "storage-position",
      "label": "Location",
      "name": "specimen.storageLocation",
      "href": (data) => {
        const specimen = data.specimen || {};
        const {storageLocation} = specimen;
        return storageLocation && storageLocation.id > 0 ? routerSvc.getUrl('ContainerDetail.Locations', {containerId: storageLocation.id}) : null;
      }
    },
    {
      "type": "text",
      "label": "Type",
      "name": "specimen.type"
    },
    {
      "type": "component",
      "component": "os-specimen-measure",
      "label": "Quantity",
      "name": "specimen.availableQty",
      "value": (data) => {
        const specimen = data.specimen || {};
        return {modelValue: specimen.availableQty, entity: 'specimen', context: data, readOnly: true};
      }
    },
    {
      "type": "text",
      "label": "Pathology",
      "name": "specimen.pathology"
    },
    {
      "type": "text",
      "label": "Anatomic Site",
      "name": "specimen.anatomicSite"
    },
    {
      "type": "text",
      "label": "Collection Container",
      "name": "specimen.collectionContainer"
    }
  ]
}
