import routerSvc    from '@/common/services/Router.js';

export default {
  key: "shipment.id",

  summary: {
    title: {
      icon: (ro) => ro.shipment.type == 'SPECIMEN' ? 'flask' : 'box-open',
      text: (ro) => '#' + ro.shipment.id + ' ' + ro.shipment.name,
      url:  (ro, query) => routerSvc.getUrl('ShipmentsListItemDetail.Overview', {shipmentId: ro.shipment.id}, query),
    },

    descriptions: [
      "shipment.status",

      (ro) => {
        let result = ro.shipment.sendingSite;
        if (ro.shipment.receivingSite) {
          result += ' | ' + ro.shipment.receivingSite;
        }

        return result;
      }
    ]
  },

  columns: [
    {
      name: "shipment.icon",
      caption: "",
      type: "component",
      component: "os-icon",
      data: (rowObject) => ({name: rowObject.shipment.type == 'SPECIMEN' ? 'flask' : 'box-open'})
    },
    {
      "name": "shipment.name",
      "captionCode": "shipments.name",
      "href": (row, query) => routerSvc.getUrl('ShipmentsListItemDetail.Overview', {shipmentId: row.rowObject.shipment.id, query}),
      "value": (rowObject) => '#' + rowObject.shipment.id + ' ' + rowObject.shipment.name
    },
    {
      "name": "shipment.sendingSite",
      "captionCode": "shipments.sending_site"
    },
    {
      "name": "shipment.sender",
      "captionCode": "shipments.sender",
      "type": "user"
    },
    {
      "name": "shipment.shippedDate",
      "captionCode": "shipments.shipped_date",
      "type": "date"
    },
    {
      "name": "shipment.receivingSite",
      "captionCode": "shipments.receiving_site"
    },
    {
      "name": "shipment.receiver",
      "captionCode": "shipments.receiver",
      "type": "user"
    },
    {
      "name": "shipment.receivedDate",
      "captionCode": "shipments.received_date",
      "type": "date"
    },
    {
      "name": "shipment.status",
      "captionCode": "shipments.status",
      "value": ({shipment: {status, request, requestStatus}}) => {
        let result = status;
        if ((status == 'PENDING' || status == 'REQUESTED') && request && requestStatus) {
          result += ' (' + requestStatus + ')';
        }

        return result;
      },
      "uiStyle": {
        "min-width": "80px"
      }
    },
    {
      "name": "shipment.specimensCount",
      "captionCode": "shipments.specimens"
    }
  ],

  filters: [
    {
      name: 'name',
      type: 'text',
      captionCode: 'shipments.name'
    },
    {
      name: 'sendingSite',
      type: 'site',
      captionCode: 'shipments.sending_site',
      listSource: {
        queryParams: {
          static: {
            listAll: true
          }
        },
        selectProp: 'name'
      }
    },
    {
      name: 'recvInstitute',
      type: 'dropdown',
      captionCode: 'shipments.receiving_institute',
      listSource: {
        apiUrl: 'institutes',
        displayProp: 'name',
        selectProp: 'name',
        searchProp: 'name'
      }
    },
    {
      name: 'recvSite',
      type: 'site',
      captionCode: 'shipments.receiving_site',
      listSource: {
        queryParams: {
          static: {
            listAll: true
          },
          dynamic: {
            institute: 'recvInstitute'
          }
        },
        selectProp: 'name'
      }
    },
    {
      name: 'labelOrBarcode',
      type: 'text',
      captionCode: 'shipments.specimen_label_or_barcode_exact'
    },
    {
      name: 'status',
      type: 'dropdown',
      captionCode: 'shipments.status',
      listSource: {
        selectProp: 'value',
        displayProp: 'value',
        options: [
          { value: 'Requested' },
          { value: 'Shipped' },
          { value: 'Received' },
          { value: 'Pending' }
        ]
      }
    }
  ]
}
