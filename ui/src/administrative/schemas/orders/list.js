
import routerSvc from '@/common/services/Router.js';

export default {
  summary: {
    title: {
      text: (ro) => '#' + ro.order.id + ' ' + ro.order.name,
      "url": (ro) => routerSvc.getUrl('OrderDetail.Overview', {orderId: ro.order.id})
    },

    descriptions: [
      "order.distributionProtocol.shortTitle",

      (ro) => {
        if (ro.order.status == 'PENDING') {
          return 'Pending';
        } else if (ro.order.status == 'EXECUTED') {
          return 'Executed';
        }

        return ''
      }
    ]
  },

  columns: [
    {
      name: "order.name",
      caption: "Name",
      href: (row) => routerSvc.getUrl('OrderDetail.Overview', {orderId: row.rowObject.order.id}),
      value: (rowObject) => '#' + rowObject.order.id + ' ' + rowObject.order.name
    },
    {
      name: "order.requester",
      caption: "Requestor",
      type: "user"
    },
    {
      name: "order.siteName",
      caption: "Site",
    },
    {
      name: "order.distributionProtocol.shortTitle",
      caption: "Distribution Protocol",
    },
    {
      name: "order.executionDate",
      caption: "Date",
      type: "date",
    },
    {
      name: "order.status",
      caption: "Status",
      value: (rowObject) => {
        if (rowObject.order.status == 'PENDING') {
          return 'Pending';
        } else if (rowObject.order.status == 'EXECUTED') {
          return 'Executed';
        }
        
        return ''
      }
    },
    {
      name: "order.specimenCnt",
      caption: "Specimens"
    }
  ],

  filters: [
    {
      name: "query",
      type: "text",
      caption: "Name"
    },
    {
      name: "requestorId",
      type: "user",
      caption: "Requestor",
      selectProp: "id"
    },
    {
      name: "requestId",
      type: "number",
      caption: "Request",
      maxFractionDigits: 0
    },
    {
      name: "receivingInstitute",
      type: "dropdown",
      caption: "Receiving Institute",
      listSource: {
        apiUrl: "institutes",
        displayProp: "name",
        selectProp: "name",
        searchProp: "name"
      }
    },
    {
      name: "dpShortTitle",
      type: "dropdown",
      caption: "Distribution Protocol",
      listSource: {
        apiUrl: "distribution-protocols",
        displayProp: "shortTitle",
        selectProp: "shortTitle",
        searchProp: "query"
      }
    },
    {
      name: "executionDate",
      type: "date",
      caption: "Execution Date",
      format: "yyyy-MM-dd"
    },
    {
      name: "status",
      type: "dropdown",
      caption: "Status",
      listSource: {
        options: [
          { name: "Pending", value: "PENDING" },
          { name: "Executed", value: "EXECUTED" }
        ],
        selectProp: "value",
        displayProp: "name"
      }
    }
  ]
}
