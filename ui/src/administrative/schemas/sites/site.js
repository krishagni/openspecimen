export default {
  fields:  [
    {
      "type": "dropdown",
      "label": "Institute",
      "name": "site.instituteName",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name"
      },
      "validations": {
        "required": {
          "message": "Institute is mandatory"
        }
      },
      "showWhen": "currentUser.admin == true"
    },

    {
      "type": "text",
      "label": "Name",
      "name": "site.name",
      "validations": {
        "required": {
          "message": "Site name is mandatory"
        }
      }
    },

    {
      "type": "text",
      "label": "Code",
      "name": "site.code"
    },

    {
      "type": "user",
      "label": "Coordinators",
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
      "label": "Type",
      "name": "site.type",
      "attribute": "site_type",
      "selectProp": "value",
      "validations": {
        "required": {
          "message": "Site type is mandatory"
        }
      }
    },

    {
      "type": "textarea",
      "label": "Address",
      "name": "site.address",
      "rows": 2
    }
  ]
}
