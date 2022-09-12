export default {
  fields:  [
    {
      "type": "dropdown",
      "labelCode": "sites.institute",
      "name": "site.instituteName",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name"
      },
      "validations": {
        "required": {
          "messageCode": "sites.institute_required"
        }
      },
      "showWhen": "currentUser.admin == true",
      "showInOverviewWhen": true
    },

    {
      "type": "text",
      "labelCode": "sites.name",
      "name": "site.name",
      "validations": {
        "required": {
          "messageCode": "sites.name_required"
        }
      }
    },

    {
      "type": "text",
      "labelCode": "sites.code",
      "name": "site.code"
    },

    {
      "type": "user",
      "labelCode": "sites.coordinators",
      "name": "site.coordinators",
      "multiple": true,
      "listSource": {
        "queryParams": {
          "dynamic": {
            "institute": "site.instituteName"
          }
        }
      }
    },

    {
      "type": "pv",
      "labelCode": "sites.type",
      "name": "site.type",
      "attribute": "site_type",
      "selectProp": "value",
      "validations": {
        "required": {
          "messageCode": "sites.type_required"
        }
      }
    },

    {
      "type": "textarea",
      "labelCode": "sites.address",
      "name": "site.address",
      "rows": 2
    }
  ]
}
