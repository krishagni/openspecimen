
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
      "caption": "Name",
      "href": (row, query) => routerSvc.getUrl('ShipmentsListItemDetail.Overview', {shipmentId: row.rowObject.shipment.id, query}),
      "value": (rowObject) => '#' + rowObject.shipment.id + ' ' + rowObject.shipment.name
    },
    {
      "name": "shipment.sendingSite",
      "caption": "Sending Site"
    },
    {
      "name": "shipment.sender",
      "caption": "Sender",
      "type": "user"
    },
    {
      "name": "shipment.shippedDate",
      "caption": "Shipped Date",
      "type": "date"
    },
    {
      "name": "shipment.receivingSite",
      "caption": "Receiving Site"
    },
    {
      "name": "shipment.receiver",
      "caption": "Receiver",
      "type": "user"
    },
    {
      "name": "shipment.receivedDate",
      "caption": "Received Date",
      "type": "date"
    },
    {
      "name": "shipment.status",
      "caption": "Status",
      "uiStyle": {
        "min-width": "80px"
      }
    },
    {
      "name": "shipment.specimensCount",
      "caption": "Specimens"
    }
  ],

  filters: [
    {
      name: 'name',
      type: 'text',
      caption: 'Name'
    },
    {
      name: 'sendingSite',
      type: 'site',
      caption: 'Sending Site',
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
      caption: 'Receiving Institute',
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
      caption: 'Receiving Site',
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
      name: 'status',
      type: 'dropdown',
      caption: 'Status',
      listSource: {
        selectProp: 'value',
        displayProp: 'value',
        options: [
          { value: 'Shipped' },
          { value: 'Received' },
          { value: 'Pending' }
        ]
      }
    }
  ]
}
