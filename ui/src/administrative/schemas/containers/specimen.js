import routerSvc from '@/common/services/Router.js';

export default {
  fields:  [
    {
      "type": "text",
      "labelCode": "containers.specimen.cp",
      "name": "specimen.cpShortTitle",
      "href": (data) => {
        const specimen = data.specimen || {};
        return routerSvc.getUrl('CpDetail.Overview', {cpId: specimen.cpId});
      }
    },
    {
      "type": "text",
      "labelCode": "containers.specimen.label",
      "name": "specimen.label",
      "href": (data) => {
        const specimen = data.specimen || {};
        return routerSvc.getUrl('SpecimenResolver', {specimenId: specimen.id});
      }
    },
    {
      "type": "text",
      "labelCode": "containers.specimen.barcode",
      "name": "specimen.barcode",
      "href": (data) => {
        const specimen = data.specimen || {};
        return routerSvc.getUrl('SpecimenResolver', {specimenId: specimen.id});
      }
    },
    {
      "type": "text",
      "labelCode": "containers.specimen.parent",
      "name": "specimen.parentLabel",
      "href": (data) => {
        const specimen = data.specimen || {};
        return specimen.parentId > 0 ? routerSvc.getUrl('SpecimenResolver', {specimenId: specimen.parentId}) : null;
      }
    },
    {
      "type": "text",
      "labelCode": "containers.specimen.ppid",
      "name": "specimen.ppid",
      "href": (data) => {
        const specimen = data.specimen || {};
        const {cpId, cprId} = specimen;
        return cprId > 0 ? routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId}) : null;
      }
    },
    {
      "type": "storage-position",
      "labelCode": "containers.specimen.location",
      "name": "specimen.storageLocation",
      "href": (data) => {
        const specimen = data.specimen || {};
        const {storageLocation} = specimen;
        return storageLocation && storageLocation.id > 0 ? routerSvc.getUrl('ContainerDetail.Locations', {containerId: storageLocation.id}) : null;
      }
    },
    {
      "type": "text",
      "labelCode": "containers.specimen.type",
      "name": "specimen.type"
    },
    {
      "type": "component",
      "component": "os-specimen-measure",
      "labelCode": "containers.specimen.available_qty",
      "name": "specimen.availableQty",
      "value": (data) => {
        const specimen = data.specimen || {};
        return {modelValue: specimen.availableQty, entity: 'specimen', context: data, readOnly: true};
      }
    },
    {
      "type": "text",
      "labelCode": "containers.specimen.pathology_status",
      "name": "specimen.pathology"
    },
    {
      "type": "text",
      "labelCode": "containers.specimen.anatomic_site",
      "name": "specimen.anatomicSite"
    },
    {
      "type": "text",
      "labelCode": "containers.specimen.collection_container",
      "name": "specimen.collectionContainer"
    }
  ]
}
