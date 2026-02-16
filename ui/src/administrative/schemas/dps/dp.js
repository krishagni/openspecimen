export default {
  fields:  [
    {
      "type": "text",
      "labelCode": "dps.title",
      "name": "dp.title",
      "validations": {
        "required": {
          "messageCode": "dps.title_req"
        }
      }
    },
    {
      "type": "text",
      "labelCode": "dps.short_title",
      "name": "dp.shortTitle",
      "validations": {
        "required": {
          "messageCode": "dps.short_title_req"
        }
      }
    },
    {
      "type": "dropdown",
      "labelCode": "dps.receiving_institute",
      "name": "dp.instituteName",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name"
      },
      "validations": {
        "required": {
          "messageCode": "dps.receiving_institute_req"
        }
      }
    },
    {
      "type": "site",
      "labelCode": "dps.receiving_site",
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
      "labelCode": "dps.principal_investigator",
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
          "messageCode": "dps.principal_investigator_req"
        }
      }
    },
    {
      "type": "user",
      "labelCode": "dps.coordinators",
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
      "labelCode": "dps.start_date",
      "name": "dp.startDate",
      "showTime": false
    },
    {
      "type": "datePicker",
      "labelCode": "dps.end_date",
      "name": "dp.endDate",
      "showTime": false
    },
    {
      "type": "text",
      "labelCode": "dps.ethics_id",
      "name": "dp.irbId"
    },
    {
      "type": "dropdown",
      "labelCode": "dps.order_custom_fields",
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
      "labelCode": "dps.order_report_query",
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
      "labelCode": "dps.email_notifs",
      "name": "dp.disableEmailNotifs",
      "options": [
        { captionCode: 'common.enabled', value: false },
        { captionCode: 'common.disabled', value: true }
      ]
    },
    {
      "type": "dropdown",
      "labelCode": "dps.order_attachment",
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
      "labelCode": "dps.label_format",
      "name": "dp.orderItemLabelFormat"
    },
    {
      "type": "subform",
      "labelCode": "dps.distributing_sites",
      "name": "dp.distributingSites",
      "fields": [
        {
          "type": "dropdown",
          "labelCode": "dps.institute",
          "name": "institute",
          "unique": "dp.distributingSites.institute",
          "listSource": {
            "apiUrl": "institutes",
            "selectProp": "name",
            "displayProp": "name",
            "searchProp": "name"
          },
          "validations": {
            "required": {
              "messageCode": "dps.institute_req"
            }
          }
        },
        {
          "type": "site",
          "labelCode": "dps.sites",
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
          },
          "validations": {
            "required": {
              "messageCode": "dps.dist_site_req"
            }
          }
        }
      ]
    },
    {
      "type": "site",
      "labelCode": "dps.distributing_sites",
      "name": "dp.distributingSites.0.sites",
      "multiple": true,
      "showWhen": "!true",
      "listSource": {
        "apiUrl": "sites",
        "idProp": "name",
        "queryParams": {
          "dynamic": {
            "institute": "currentUser.instituteName"
          }
        }
      },
      "validations": {
        "required": {
          "messageCode": "dps.dist_site_req"
        }
      }
    }
  ]
}
