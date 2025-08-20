import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "specimen.cpShortTitle",
      captionCode: "cps.cp",
      href: ({rowObject: {specimen}}) => routerSvc.getUrl('CpDetail.Overview', {cpId: specimen.cpId}),
      hrefTarget: "_blank"
    },
    {
      name: "specimenLabel",
      captionCode: "cps.specimen",
      value: ({specimen}) => specimen.label + (specimen.barcode ? ' (' + specimen.barcode + ')' : ''),
      href: ({rowObject: {specimen}}) => routerSvc.getUrl('SpecimenResolver', {specimenId: specimen.id}),
      hrefTarget: "_blank"
    },
    {
      name: "specimen.storageLocation.hierarchy",
      captionCode: "containers.container_hierarchy",
      uiStyle: {
        "max-width": "350px"
      }
    },
    {
      name: "specimen.availableQty",
      captionCode: "specimens.available_quantity",
      type: "specimen-measure",
      entity: "specimen"
    },
    {
      name: "specimenLocation",
      captionCode: "specimens.location",
      value: ({specimen: {storageLocation}}) => {
        if (storageLocation && storageLocation.name) {
          return storageLocation.name + (storageLocation.formattedPosition ? ' (' + storageLocation.formattedPosition + ')' : '');
        }

        return null;
      },
      uiStyle: {
        "max-width": "350px"
      }
    }
  ],

  filters: [
    {
      name: "container",
      captionCode: "specimens.container",
      type: "dropdown",
      listSource: {
        apiUrl: "storage-containers",
        displayProp: ({displayName, name}) => displayName ? displayName + " (" + name + ")" : name,
        selectProp: "name",
        searchProp: "name"
      }
    }
  ]
}
