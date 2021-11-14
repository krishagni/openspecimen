
import routerSvc    from '@/common/services/Router.js';

export default {
  columns: [
    {
      "name": "shipment.name",
      "caption": "Name",
      "href": (row) => routerSvc.getUrl('ShipmentOverview', {shipmentId: row.rowObject.shipment.id}),
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
      "name": "shipment.receivingSite",
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
      "caption": "Status"
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
      caption: 'Institute',
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
