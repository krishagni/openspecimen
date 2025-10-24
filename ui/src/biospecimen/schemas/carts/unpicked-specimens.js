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
      name: "specimen.type",
      captionCode: "specimens.type",
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
      captionCode: "specimens.quantity",
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
      name: "cp",
      captionCode: "specimens.cp",
      type: "dropdown",
      listSource: {
        apiUrl: "lists/expression-values",
        queryParams: {
          static: {
            listName: "cart-specimens-list-view",
            expr: "CollectionProtocol.shortTitle"
          },
          dynamic: {
            objectId: "listViewCtx.cart.id"
          }
        },
        optionsFn: options => options.map(value => ({value})),
        searchProp: "searchTerm",
        displayProp: "value",
        selectProp: "value"
      }
    },
    {
      name: "type",
      captionCode: "specimens.type",
      type: "dropdown",
      listSource: {
        apiUrl: "lists/expression-values",
        queryParams: {
          static: {
            listName: "cart-specimens-list-view",
            expr: "Specimen.type"
          },
          dynamic: {
            objectId: "listViewCtx.cart.id"
          }
        },
        optionsFn: options => options.map(value => ({value})),
        searchProp: "searchTerm",
        displayProp: "value",
        selectProp: "value"
      }
    },
    {
      name: "container",
      captionCode: "specimens.freezer",
      type: "dropdown",
      listSource: {
        apiUrl: "lists/expression-values",
        queryParams: {
          static: {
            listName: "cart-specimens-list-view",
            expr: "Specimen.specimenPosition.ancestors.root_container_name"
          },
          dynamic: {
            objectId: "listViewCtx.cart.id"
          }
        },
        optionsFn: options => options.map(value => ({value})),
        searchProp: "searchTerm",
        displayProp: "value",
        selectProp: "value"
      }
    }
  ]
}
