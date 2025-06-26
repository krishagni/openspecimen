
import routerSvc from '@/common/services/Router.js';

export default {
  summary: {
    title: {
      text: (ro) => '#' + ro.order.id + ' ' + ro.order.name,
      url: (ro, query) => routerSvc.getUrl('OrdersListItemDetail.Overview', {orderId: ro.order.id}, query)
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
      captionCode: "orders.name",
      href: (row, query) => routerSvc.getUrl('OrdersListItemDetail.Overview', {orderId: row.rowObject.order.id}, query),
      value: (rowObject) => '#' + rowObject.order.id + ' ' + rowObject.order.name
    },
    {
      name: "order.requester",
      captionCode: "orders.requester",
      type: "user"
    },
    {
      name: "order.siteName",
      captionCode: "orders.site",
    },
    {
      name: "order.distributionProtocol.shortTitle",
      captionCode: "orders.dp",
    },
    {
      name: "order.executionDate",
      captionCode: "orders.date",
      type: "date"
    },
    {
      name: "order.status",
      captionCode: "orders.status",
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
      captionCode: "orders.specimens"
    }
  ],

  filters: [
    {
      name: "query",
      type: "text",
      captionCode: "orders.name"
    },
    {
      name: "requestorId",
      type: "user",
      captionCode: "orders.requester",
      selectProp: "id"
    },
    {
      name: "requestId",
      type: "number",
      captionCode: "orders.request",
      maxFractionDigits: 0
    },
    {
      name: "receivingInstitute",
      type: "dropdown",
      captionCode: "orders.receiving_institute",
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
      captionCode: "orders.dp",
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
      captionCode: "orders.execution_date",
      dateOnly: true
    },
    {
      name: "status",
      type: "dropdown",
      captionCode: "orders.status",
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
