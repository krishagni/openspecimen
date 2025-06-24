
import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      "name": "specimenLabel",
      "labelCode": "shipments.specimen.label",
      "type": "span",
      "showWhen": "!receive || !allowSpecimenRelabeling",
      "href": (rowObject) => routerSvc.getUrl('SpecimenResolver', {specimenId: rowObject.specimen.id}),
      "value": (rowObject) => {
        let spmn = rowObject.specimen;
        let label = spmn.label;
        if (spmn.barcode) {
          label += ' (' + spmn.barcode + ')';
        }

        return label;
      },
      "sortable": true
    },
    {
      "name": "specimen.label",
      "labelCode": "shipments.specimen.label",
      "type": "text",
      "showWhen": "receive && allowSpecimenRelabeling",
      "validations": {
        "required": {
          "messageCode": "shipments.specimen.label_required"
        }
      },
      "sortable": true
    },
    {
      "name": "specimen.externalIds",
      "labelCode": "shipments.specimen.external_id",
      "type": "span",
      "value": (rowObject) => {
        let externalIds = rowObject.specimen.externalIds;
        if (!externalIds || externalIds.length == 0) {
          return '-';
        }

        return externalIds.map(externalId => externalId.value + ' (' + externalId.name + ')').join(', ');
      },
      "showWhen": (context) => {
        return context.specimenItems.some(item => item.specimen.externalIds && item.specimen.externalIds.length > 0)
      },
      "sortable": true
    },
    {
      "name": "specimen.type",
      "labelCode": "shipments.specimen.type",
      "type": "span"
    },
    {
      "name": "specimen.cpShortTitle",
      "labelCode": "shipments.specimen.cp",
      "type": "span",
      "sortable": true
    },
    {
      "name": "specimen.ppid",
      "labelCode": "shipments.specimen.ppid",
      "type": "span",
      "showWhen": (context) => context.specimenItems.some(item => typeof item.specimen.ppid == 'string'),
      "sortable": true
    },
    {
      "name": "specimen.externalIdName",
      "labelCode": "shipments.specimen.external_id_name",
      "type": "text",
      "showWhen": "receive && allowExtIdName",
      "validations": {
        "requiredIf": {
          "expr": "!!specimen.externalIdValue",
          "messageCode": "shipments.external_id_name_req"
        }
      }
    },
    {
      "name": "specimen.externalIdValue",
      "labelCode": "shipments.specimen.external_id_value",
      "type": "text",
      "showWhen": "receive && allowExtIdValue",
      "validations": {
        "requiredIf": {
          "expr": "!!specimen.externalIdName",
          "messageCode": "shipments.external_id_value_req"
        }
      }
    },
    {
      "name": "specimen.availableQty",
      "labelCode": "shipments.specimen.quantity",
      "type": "specimen-measure",
      "entity": "specimen",
      "measure": "quantity",
      "readOnly": "!receive"
    },
    {
      "name": "specimen.storageLocation",
      "labelCode": "shipments.specimen.location",
      "type": "span",
      "displayType": "storage-position",
      "showWhen": "!receive"
    },
    {
      "name": "specimen.storageLocation",
      "labelCode": "shipments.specimen.location",
      "type": "storage-position",
      "listSource": {
        "queryParams": {
          "static": {
            "entityType": "specimen"
          },
          "dynamic": {
            "entity": "specimen",
            "site": "shipment.receivingSite"
          }
        }
      },
      "showWhen": "receive",
      "uiStyle": {
        "min-width": "300px"
      },
      "enableCopyFirstToAll": true
    },
    {
      "name": "receivedQuality",
      "labelCode": "shipments.received_quality",
      "type": "pv",
      "attribute": "shipment_item_received_quality",
      "selectProp": "value",
      "showWhen": "receive || shipment.status == 'Received'",
      "validations": {
        "required": {
          "messageCode": "shipments.received_quality_required"
        }
      },
      "uiStyle": {
        "min-width": "175px"
      },
      "enableCopyFirstToAll": true
    },
    {
      "name": "specimen.printLabel",
      "icon": "print",
      "type": "booleanCheckbox",
      "tooltip": "Print Labels",
      "enableCopyFirstToAll": true,
      "showWhen": "receive",
      "uiStyle": {
        "min-width": "30px"
      }
    }
  ]
}
