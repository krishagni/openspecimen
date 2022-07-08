import routerSvc from '@/common/services/Router.js';

export default {
  "fields":  [
    {
      "type": "radio",
      "label": "Used For",
      "name": "container.usedFor",
      "options": [
        { "caption": "Storage", "value": "STORAGE" },
        { "caption": "Distribution", "value": "DISTRIBUTION" }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "message": "Used for is mandatory"
        }
      }
    },
    {
      "type": "dropdown",
      "label": "Type",
      "name": "container.typeName",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
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
      "label": "Display Name",
      "name": "container.displayName",
      "href": (data) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: data.container.id}),
      "summary": true
    },
    {
      "type": "text",
      "label": "Unique Name",
      "name": "container.name",
      "validations": {
        "required": {
          "message": "Container unique name is mandatory"
        }
      },
      "href": (data) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: data.container.id}),
      "summary": true
    },
    {
      "type": "text",
      "label": "Barcode",
      "name": "container.barcode",
      "summary": true
    },
    {
      "type": "number",
      "label": "Temperature (Celsius)",
      "name": "container.temperature"
    },
    {
      "type": "site",
      "label": "Parent Site",
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
          "message": "Parent Site is mandatory"
        }
      }
    },
    {
      "type": "storage-position",
      "label": "Location",
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
      }
    },
    {
      "type": "text",
      "label": "Dimension",
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
      "label": "Rows",
      "name": "container.noOfRows",
      "validations": {
        "required": {
          "message": "Rows is mandatory"
        }
      },
      "dataEntry": true
    },
    {
      "type": "number",
      "label": "Columns",
      "name": "container.noOfColumns",
      "validations": {
        "required": {
          "message": "Columns is mandatory"
        }
      },
      "dataEntry": true
    },
    {
      "type": "text",
      "label": "Stored",
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
      "label": "Utilisation",
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
      "label": "Position Labeling",
      "name": "container.positionLabelingMode",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "options": [
        { "caption": "Linear", "value": "LINEAR" },
        { "caption": "Row and Column", "value": "TWO_D" }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "message": "Position Labeling is mandatory"
        }
      }
    },
    {
      "type": "text",
      "label": "Labeling Scheme",
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
      "label": "Row Labeling Scheme",
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
          "message": "Row Labeling Scheme is mandatory"
        }
      },
      "dataEntry": true
    },
    {
      "type": "dropdown",
      "label": "Column Labeling Scheme",
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
          "message": "Column Labeling Scheme is mandatory"
        }
      },
      "dataEntry": true
    },
    {
      "type": "dropdown",
      "label": "Position Assignment",
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
        "required": "Position Assignment is mandatory"
      }
    },
    {
      "type": "radio",
      "label": "Store Specimens?",
      "name": "container.storeSpecimensEnabled",
      "options": [
        { caption: 'Yes', value: true },
        { caption: 'No', value: false }
      ],
      "optionsPerRow": 2,
      "summary": true
    },
    {
      "type": "radio",
      "label": "Display in Map",
      "name": "container.cellDisplayProp",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "options": [
        { caption: "Specimen Label", value: "SPECIMEN_LABEL" },
        { caption: "Specimen Barcode", value: "SPECIMEN_BARCODE" },
        { caption: "PPID", value: "SPECIMEN_PPID" }
      ],
      "optionsPerRow": 3,
    },
    {
      "type": "text",
      "label": "Collection Protocols",
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
      "label": "Specimen Types",
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
      "label": "Distribution Protocols",
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
      "label": "Free Locations",
      "name": "container.freePositions",
      "showInOverviewWhen": "container.noOfRows > 0 && container.noOfColumns > 0",
      "summary": true
    }
  ]
}
