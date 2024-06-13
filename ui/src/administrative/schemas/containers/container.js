import routerSvc from '@/common/services/Router.js';

export default {
  "fields":  [
    {
      "type": "radio",
      "labelCode": "containers.used_for",
      "name": "container.usedFor",
      "options": [
        { "captionCode": "containers.used_for_storage",      "value": "STORAGE" },
        { "captionCode": "containers.used_for_distribution", "value": "DISTRIBUTION" }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "messageCode": "containers.used_for_required"
        }
      }
    },
    {
      "type": "dropdown",
      "labelCode": "containers.type",
      "name": "container.typeName",
      "href": (data) => {
        const container = data.container;
        if (container.typeId > 0) {
          return routerSvc.getUrl('ContainerTypeDetail.Overview', {typeId: container.typeId});
        }

        return null;
      },
      "listSource": {
        "apiUrl" : "container-types",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name"
      },
      "summary": true
    },
    {
      "type": "text",
      "labelCode": "containers.display_name",
      "name": "container.displayName",
      "href": (data) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: data.container.id}),
      "summary": true
    },
    {
      "type": "text",
      "labelCode": "containers.unique_name",
      "name": "container.name",
      "validations": {
        "required": {
          "messageCode": "containers.unique_name_required"
        }
      },
      "href": (data) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: data.container.id}),
      "summary": true
    },
    {
      "type": "text",
      "labelCode": "containers.barcode",
      "name": "container.barcode",
      "summary": true
    },
    {
      "type": "number",
      "labelCode": "containers.temperature",
      "name": "container.temperature"
    },
    {
      "type": "site",
      "labelCode": "containers.site",
      "name": "container.siteName",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name",
        "queryParams": {
          "static": {
            "resource": "StorageContainer",
            "operation": "Update"
          }
        }
      },
      "validations": {
        "required": {
          "messageCode": "containers.site_required"
        }
      }
    },
    {
      "type": "storage-position",
      "labelCode": "containers.parent_container",
      "name": "container.storageLocation",
      "showInOverviewWhen": "container.storageLocation && container.storageLocation.name",
      "listSource": {
        "queryParams": {
          "static": {
            "entityType": "storage_container"
          },
          "dynamic": {
            "entity": "container",
            "site": "container.siteName",
            "usageMode": "container.usedFor"
          }
        }
      },
      "href": ({container}) => {
        if (container.storageLocation && container.storageLocation.id > 0) {
          return routerSvc.getUrl('ContainerDetail.Overview', {containerId: container.storageLocation.id});
        }

        return null;
      }
    },
    {
      "type": "text",
      "labelCode": "containers.dimension",
      "name": "container.uiDimension",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "value": ({container}) => {
        if (container.noOfRows > 0 && container.noOfColumns > 0) {
          return container.noOfRows + ' x ' + container.noOfColumns;
        }

        return null;
      },
      "summary": true
    },
    {
      "type": "number",
      "labelCode": "containers.rows",
      "name": "container.noOfRows",
      "validations": {
        "required": {
          "messageCode": "containers.rows_required"
        }
      },
      "dataEntry": true
    },
    {
      "type": "number",
      "labelCode": "containers.columns",
      "name": "container.noOfColumns",
      "validations": {
        "required": {
          "messageCode": "containers.columns_required"
        }
      },
      "dataEntry": true
    },
    {
      "type": "text",
      "labelCode": "containers.stored_specimens",
      "name": "container.uiStored",
      "showInOverviewWhen": "!container.storageLocation || !container.storageLocation.name",
      "value": ({container}) => {
        if (container.storageLocation && container.storageLocation.name) {
          return null;
        }

        if (container.capacity > 0) {
          return new Number(container.storedSpecimens).toLocaleString() +
            ' specimens of ' + new Number(container.capacity).toLocaleString();
        } else {
          return new Number(container.storedSpecimens).toLocaleString() + ' specimens';
        }
      }
    },
    {
      "type": "component",
      "component": "os-utilisation-bar",
      "labelCode": "containers.utilisation",
      "name": "container.uiUtilisation",
      "showInOverviewWhen": "!container.storageLocation || !container.storageLocation.name",
      "value": ({container}) => {
        if (container.capacity > 0 && (!container.storageLocation || !container.storageLocation.name)) {
          return {utilisation: Math.round(container.storedSpecimens / container.capacity * 100)};
        }

        return null;
      }
    },
    {
      "type": "radio",
      "labelCode": "containers.position_labeling",
      "name": "container.positionLabelingMode",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "options": [
        { "captionCode": "containers.position_labeling_linear", "value": "LINEAR" },
        { "captionCode": "containers.position_labeling_two_d", "value": "TWO_D" }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "messageCode": "containers.position_labeling_required"
        }
      }
    },
    {
      "type": "text",
      "labelCode": "containers.labeling_scheme",
      "name": "container.uiLabelingScheme",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "value": ({container}) => {
        if (container.positionLabelingMode == 'TWO_D') {
          return container.rowLabelingScheme + ' x ' + container.columnLabelingScheme;
        }

        return null;
      },
      "summary": true
    },
    {
      "type": "dropdown",
      "labelCode": "containers.row_labeling_scheme",
      "name": "container.rowLabelingScheme",
      "listSource": {
        "options": [
          { "name": "Numbers" },
          { "name": "Alphabets Upper Case" },
          { "name": "Alphabets Lower Case" },
          { "name": "Roman Upper Case" },
          { "name": "Roman Lower Case" },
        ],
        "selectProp": "name",
        "displayProp": "name"
      },
      "showWhen": "container.positionLabelingMode == 'TWO_D'",
      "validations": {
        "required": {
          "messageCode": "containers.row_labeling_scheme_required"
        }
      },
      "dataEntry": true
    },
    {
      "type": "dropdown",
      "labelCode": "containers.column_labeling_scheme",
      "name": "container.columnLabelingScheme",
      "listSource": {
        "options": [
          { "name": "Numbers" },
          { "name": "Alphabets Upper Case" },
          { "name": "Alphabets Lower Case" },
          { "name": "Roman Upper Case" },
          { "name": "Roman Lower Case" },
        ],
        "selectProp": "name",
        "displayProp": "name"
      },
      "showWhen": "container.positionLabelingMode == 'TWO_D'",
      "validations": {
        "required": {
          "messageCode": "containers.column_labeling_scheme_required"
        }
      },
      "dataEntry": true
    },
    {
      "type": "dropdown",
      "labelCode": "containers.position_assignment",
      "name": "container.positionAssignment",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "listSource": {
        "options": [
          {"name": "HZ_TOP_DOWN_LEFT_RIGHT",  "value": "Horizontal, top to down, left to right"},
          {"name": "HZ_TOP_DOWN_RIGHT_LEFT",  "value": "Horizontal, top to down, right to left"},
          {"name": "HZ_BOTTOM_UP_LEFT_RIGHT", "value": "Horizontal, bottom to top, left to right"},
          {"name": "HZ_BOTTOM_UP_RIGHT_LEFT", "value": "Horizontal, bottom to top, right to left"},
          {"name": "VT_TOP_DOWN_LEFT_RIGHT",  "value": "Vertical, top to down, left to right"},
          {"name": "VT_TOP_DOWN_RIGHT_LEFT",  "value": "Vertical, top to down, right to left"},
          {"name": "VT_BOTTOM_UP_LEFT_RIGHT", "value": "Vertical, bottom to top, left to right"},
          {"name": "VT_BOTTOM_UP_RIGHT_LEFT", "value": "Vertical, bottom to top, right to left"}
        ],
        "selectProp": "name",
        "displayProp": "value"
      },
      "validations": {
        "required": {
          "messageCode": "containers.position_assignment_required"
        }
      }
    },
    {
      "type": "radio",
      "labelCode": "containers.store_specimens_q",
      "name": "container.storeSpecimensEnabled",
      "options": [
        { captionCode: 'common.yes', value: true },
        { captionCode: 'common.no',  value: false }
      ],
      "optionsPerRow": 2,
      "summary": true
    },
    {
      "type": "radio",
      "labelCode": "containers.automated_freezer_q",
      "name": "container.automated",
      "options": [
        { captionCode: 'common.yes', value: true },
        { captionCode: 'common.no',  value: false }
      ],
      "optionsPerRow": 2,
      "showWhen": "container.storeSpecimensEnabled && !container.noOfRows && !container.noOfColumns",
      "showInOverviewWhen": "container.storeSpecimensEnabled && !container.noOfRows && !container.noOfColumns"
    },
    {
      "type": "dropdown",
      "labelCode": "containers.automated_provider",
      "name": "container.autoFreezerProvider",
      "listSource": {
        "apiUrl" : "auto-freezer-providers",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name"
      },
      "showWhen": "container.automated",
      "showInOverviewWhen": "container.automated"
    },
    {
      "type": "radio",
      "labelCode": "containers.display_in_map",
      "name": "container.cellDisplayProp",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "options": [
        { captionCode: "containers.specimen_label", value: "SPECIMEN_LABEL" },
        { captionCode: "containers.specimen_barcode", value: "SPECIMEN_BARCODE" },
        { captionCode: "containers.ppid", value: "SPECIMEN_PPID" }
      ],
      "optionsPerRow": 3,
    },
    {
      "type": "text",
      "labelCode": "containers.collection_protocols",
      "name": "contaner.uiCps",
      "showInOverviewWhen": "container.usedFor == 'STORAGE'",
      "value": ({container}) => {
        if (container.usedFor == 'STORAGE') {
          if (container.calcAllowedCollectionProtocols.length > 0) {
            return container.calcAllowedCollectionProtocols.join(', ');
          } else {
            return 'All';
          }
        }

        return null;
      }
    },
    {
      "type": "text",
      "labelCode": "containers.specimen_types",
      "name": "contaner.uiSpecimenTypes",
      "showInOverviewWhen": "container.usedFor == 'STORAGE'",
      "value": ({container}) => {
        if (container.usedFor == 'STORAGE') {
          let types = [];
          if (container.calcAllowedSpecimenClasses.length > 0) {
            Array.prototype.push.apply(types, container.calcAllowedSpecimenClasses);
          }

          if (container.calcAllowedSpecimenTypes.length > 0) {
            Array.prototype.push.apply(types, container.calcAllowedSpecimenTypes);
          }

          if (types.length > 0) {
            return types.join(', ');
          }
        }

        return null;
      }
    },
    {
      "type": "text",
      "labelCode": "containers.distribution_protocols",
      "name": "contaner.uiDps",
      "showInOverviewWhen": "container.usedFor == 'DISTRIBUTION'",
      "value": ({container}) => {
        if (container.usedFor == 'DISTRIBUTION') {
          if (container.calcAllowedDistributionProtocols.length > 0) {
            return container.calcAllowedDistributionProtocols.join(', ');
          } else {
            return 'All';
          }
        }

        return null;
      }
    },
    {
      "type": "number",
      "labelCode": "containers.free_locations",
      "name": "container.freePositions",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "summary": true
    }
  ]
}
