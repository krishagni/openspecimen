import routerSvc from '@/common/services/Router.js';

export default {
  fields:  [
    {
      "type": "span",
      "label": "Request",
      "name": "order.request.id",
      "showWhen": "!!order.request.id",
      "href": (data) => {
        const request = data.order.request;
        if (request && request.id) {
          return routerSvc.ngUrl('specimen-requests/' + request.id + '/overview', {catalogId: request.catalogId});
        }

        return null;
      }
    },
    {
      "type": "dropdown",
      "label": "Distribution Protocol",
      "name": "order.distributionProtocol",
      "listSource": {
        "apiUrl": "distribution-protocols",
        "displayProp": "shortTitle",
        "searchProp": "query"
      },
      "disableWhen": "order.status != 'PENDING'",
      "validations": {
        "required": {
          "message": "Distribution protocol is mandatory"
        }
      },
      "href": (data) => routerSvc.getUrl('DpDetail.Overview', {dpId: data.order.distributionProtocol.id})
    },
    {
      "type": "text",
      "label": "Name",
      "name": "order.name",
      "validations": {
        "required": {
          "message": "Order name is mandatory"
        }
      }
    },
    {
      "type": "dropdown",
      "label": "Receiving Institute",
      "name": "order.instituteName",
      "listSource": {
        "apiUrl": "institutes",
        "displayProp": "name",
        "selectProp": "name",
        "searchProp": "name"
      },
      "validations": {
        "required": {
          "message": "Receiving institute is mandatory"
        }
      }
    },
    {
      "type": "site",
      "label": "Receiving Site",
      "name": "order.siteName",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name",
        "queryParams": {
          "dynamic": {
            "institute": "order.instituteName"
          },
          "static": {
            "listAll": true
          }
        }
      },
    },
    {
      "type": "user",
      "label": "Requestor",
      "name": "order.requester",
      "validations": {
        "required": {
          "message": "Requestor is mandatory"
        }
      }
    },
    {
      "type": "datePicker",
      "label": "Distribution Date",
      "name": "order.executionDate",
      "showTime": true,
      "validations": {
        "required": {
          "message": "Distribution Date is mandatory"
        }
      }
    },
    {
      "type": "text",
      "label": "Tracking URL",
      "name": "order.trackingUrl"
    },
    {
      "type": "textarea",
      "label": "Sender Comments",
      "name": "order.comments"
    },
    {
      "type": "user",
      "label": "Distributor",
      "name": "order.distributor"
    }
  ]
}
