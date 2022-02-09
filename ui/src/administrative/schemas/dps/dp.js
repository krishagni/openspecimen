export default {
  fields:  [
    {
      "type": "text",
      "label": "Title",
      "name": "dp.title",
      "validations": {
        "required": {
          "message": "Title is mandatory"
        }
      }
    },
    {
      "type": "text",
      "label": "Short Title",
      "name": "dp.shortTitle",
      "validations": {
        "required": {
          "message": "Short title is mandatory"
        }
      }
    },
    {
      "type": "dropdown",
      "label": "Receiving Institute",
      "name": "dp.instituteName",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name",
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
      "name": "dp.defReceivingSiteName",
      "listSource": {
        "selectProp": "name",
        "queryParams": {
          "dynamic": {
            "institute": "dp.instituteName"
          },
          "static": {
            "listAll": true
          }
        }
      }
    },
    {
      "type": "user",
      "label": "Principal Investigator",
      "name": "dp.principalInvestigator",
      "listSource": {
        "queryParams": {
          "dynamic": {
            "institute": "dp.instituteName"
          }
        }
      },
      "validations": {
        "required": {
          "message": "Principal Investigator is mandatory"
        }
      }
    },
    {
      "type": "user",
      "label": "Coordinators",
      "name": "dp.coordinators",
      "multiple": true,
      "listSource": {
        "queryParams": {
          "dynamic": {
            "institute": "dp.instituteName"
          }
        }
      }
    },
    {
      "type": "datePicker",
      "label": "Start Date",
      "name": "dp.startDate",
      "showTime": false
    },
    {
      "type": "datePicker",
      "label": "End Date",
      "name": "dp.endDate",
      "showTime": false
    },
    {
      "type": "text",
      "label": "Ethics ID",
      "name": "dp.irbId"
    },
    {
      "type": "dropdown",
      "label": "Order Custom Fields",
      "name": "dp.orderExtnForm",
      "listSource": {
        "apiUrl": "forms",
        "displayProp": "caption",
        "searchProp": "name",
        "idProp": "formId"
      }
    },
    {
      "type": "dropdown",
      "label": "Order Report Query",
      "name": "dp.report",
      "listSource": {
        "apiUrl": "saved-queries",
        "displayProp": "title",
        "searchProp": "searchString",
        "queryParams": {
          "static": {
            "returnList": true
          }
        }
      }
    },
    {
      "type": "radio",
      "label": "Email Notifications",
      "name": "dp.disableEmailNotifs",
      "options": [
        { caption: 'Enabled', value: false },
        { caption: 'Disabled', value: true }
      ]
    },
    {
      "type": "dropdown",
      "label": "Order Attachment",
      "name": "dp.attachmentType",
      "listSource": {
        "selectProp": "name",
        "displayProp": "caption",
        "options": [
          { "name": "NONE",       "caption": "None" },
          { "name": "CSV_REPORT", "caption": "CSV Report" },
          { "name": "MANIFEST",   "caption": "Manifest (PDF)" },
          { "name": "BOTH",       "caption": "Both CSV Report and Manifest" }
        ]
      },
      "displayValues": {
        "NONE": "None",
        "CSV_REPORT": "CSV Report",
        "MANIFEST": "Manifest (PDF)",
        "BOTH": "Both CSV Report and Manifest"
      }
    },
    {
      "type": "text",
      "label": "Label Format",
      "name": "dp.orderItemLabelFormat"
    },
    {
      "type": "subform",
      "label": "Distributing Sites",
      "name": "dp.distributingSites",
      "showWhen": "currentUser.admin",
      "fields": [
        {
          "type": "dropdown",
          "label": "Institute",
          "name": "institute",
          "listSource": {
            "apiUrl": "institutes",
            "selectProp": "name",
            "displayProp": "name",
            "searchProp": "name"
          }
        },
        {
          "type": "site",
          "label": "Sites",
          "name": "sites",
          "multiple": true,
          "listSource": {
            "apiUrl": "sites",
            "idProp": "name",
            "queryParams": {
              "dynamic": {
                "institute": "institute"
              }
            }
          }
        }
      ]
    },
    {
      "type": "site",
      "label": "Distributing Sites",
      "name": "dp.distributingSites.0.sites",
      "multiple": true,
      "showWhen": "!currentUser.admin",
      "listSource": {
        "apiUrl": "sites",
        "idProp": "name",
        "queryParams": {
          "dynamic": {
            "institute": "currentUser.instituteName"
          }
        }
      }
    }
  ]
}
