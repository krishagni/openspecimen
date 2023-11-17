import routerSvc from '@/common/services/Router.js';

export default {
  fields:  [
    {
      "type": "span",
      "labelCode": "orders.request",
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
      "labelCode": "orders.dp",
      "name": "order.distributionProtocol",
      "listSource": {
        "apiUrl": "distribution-protocols",
        "displayProp": "shortTitle",
        "searchProp": "query",
        "queryParams": {
          "dynamic": {
            "cp": "cpShortTitle"
          }
        }
      },
      "disableWhen": "order.status != 'PENDING'",
      "validations": {
        "required": {
          "messageCode": "orders.dp_req"
        }
      },
      "href": (data) => routerSvc.getUrl('DpDetail.Overview', {dpId: data.order.distributionProtocol.id})
    },
    {
      "type": "text",
      "labelCode": "orders.name",
      "name": "order.name",
      "validations": {
        "required": {
          "messageCode": "orders.name_req"
        }
      }
    },
    {
      "type": "dropdown",
      "labelCode": "orders.receiving_institute",
      "name": "order.instituteName",
      "listSource": {
        "apiUrl": "institutes",
        "displayProp": "name",
        "selectProp": "name",
        "searchProp": "name"
      },
      "validations": {
        "required": {
          "messageCode": "orders.receiving_institute_req"
        }
      }
    },
    {
      "type": "site",
      "labelCode": "orders.receiving_site",
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
      "labelCode": "orders.requester",
      "name": "order.requester",
      "listSource": {
        "queryParams": {
          "dynamic": {
            "institute": "order.instituteName"
          }
        }
      },
      "validations": {
        "required": {
          "messageCode": "orders.requester_req"
        }
      }
    },
    {
      "type": "datePicker",
      "labelCode": "orders.distribution_date",
      "name": "order.executionDate",
      "showTime": true,
      "validations": {
        "required": {
          "messageCode": "orders.distribution_date_req"
        }
      }
    },
    {
      "type": "text",
      "labelCode": "orders.tracking_url",
      "name": "order.trackingUrl"
    },
    {
      "type": "textarea",
      "labelCode": "orders.sender_comments",
      "name": "order.comments"
    },
    {
      "type": "user",
      "labelCode": "orders.distributor",
      "name": "order.distributor"
    },
    {
      "type": "booleanCheckbox",
      "labelCode": "orders.checkout_specimens",
      "name": "order.checkout",
      "options": [
        { "value": true, "captionCode": "common.yes" },
        { "value": false, "captionCode": "common.no" }
      ]
    }
  ]
}
