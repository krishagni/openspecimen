import http from "@/common/services/HttpClient.js"

export default {
  fields:  [
    {
      "type": "site",
      "multiple": true,
      "labelCode": "cps.sites",
      "name": "cp.sites",
      "listSource": {
        "selectProp": "name",
        "displayProp": "name",
        "queryParams": {
          "static": {
            "resource": "CollectionProtocol",
            "op": "Update"
          }
        }
      },
      "validations": {
        "required": {
          "messageCode": "cps.sites_req"
        }
      }
    },
    {
      "type": "subform",
      "labelCode": "cps.sites",
      "name": "cp.cpSites",
      "fields": [
        {
          "type": "span",
          "labelCode": "cps.site",
          "name": "siteName"
        },
        {
          "type": "text",
          "labelCode": "cps.code",
          "name": "code"
        }
      ],
      "readOnlyCollection": true,
      "showWhen": "showSiteCodeInfo"
    },
    {
      "type": "text",
      "labelCode": "cps.title",
      "name": "cp.title",
      "validations": {
        "required": {
          "messageCode": "cps.title_req"
        }
      }
    },
    {
      "type": "text",
      "labelCode": "cps.short_title",
      "name": "cp.shortTitle",
      "validations": {
        "required": {
          "messageCode": "cps.short_title_req"
        }
      }
    },
    {
      "type": "text",
      "labelCode": "cps.code",
      "name": "cp.code"
    },
    {
      "type": "user",
      "labelCode": "cps.pi",
      "name": "cp.principalInvestigator",
      "validations": {
        "required": {
          "messageCode": "cps.pi_req"
        }
      }
    },
    {
      "type": "user",
      "multiple": true,
      "labelCode": "cps.coordinators",
      "name": "cp.coordinators"
    },
    {
      "type": "datePicker",
      "labelCode": "cps.start_date",
      "name": "cp.startDate"
    },
    {
      "type": "datePicker",
      "labelCode": "cps.end_date",
      "name": "cp.endDate"
    },
    {
      "type": "text",
      "labelCode": "cps.irb_id",
      "name": "cp.irbId"
    },
    {
      "type": "radio",
      "labelCode": "cps.type",
      "name": "cp.specimenCentric",
      "options": [
        {
          "captionCode": "cps.participant_centric",
          "value": false
        },
        {
          "captionCode": "cps.specimen_centric",
          "value": true
        }
      ],
      "optionsPerRow": 2,
      "showWhen": "!cp.id"
    },
    {
      "type": "number",
      "labelCode": "cps.expected_registrations",
      "name": "cp.anticipatedParticipantsCount",
      "maxFractionDigits": 0,
      "showWhen": "!cp.specimenCentric",
      "showInOverviewWhen": "!cp.specimenCentric"
    },
    {
      "type": "text",
      "labelCode": "cps.sop_doc_url",
      "name": "cp.sopDocumentUrl",
      "showWhen": "!cp.sopDocumentName"
    },
    {
      "type": "fileUpload",
      "labelCode": "cps.sop_doc",
      "name": "cp.sopDocumentName",
      "url": http.getUrl("collection-protocols/sop-documents"),
      "headers": http.headers,
      "value": ({cp}) => {
        if (cp.sopDocumentName) {
          const idx = cp.sopDocumentName.indexOf('_');
          return cp.sopDocumentName.substring(idx + 1);
        }

        return cp.sopDocumentName;
      },
      "href": ({cp}) => http.getUrl("collection-protocols/" + cp.id + "/sop-document"),
      "showWhen": "!cp.sopDocumentUrl",
    },
    {
      "type": "radio",
      "labelCode": "cps.store_spr",
      "name": "cp.storeSprEnabled",
      "options": [
        {
          "captionCode": "common.yes",
          "value": true
        },
        {
          "captionCode": "common.no",
          "value": false
        },
        {
          "captionCode": "cps.use_system_setting",
          "value": "use_system_setting"
        }
      ],
      "optionsPerRow": 3,
      "showWhen": "!cp.specimenCentric",
      "showInOverviewWhen": "!cp.specimenCentric"
    },
    {
      "type": "radio",
      "labelCode": "cps.extract_spr_text",
      "name": "cp.extractSprText",
      "options": [
        {
          "captionCode": "common.yes",
          "value": true
        },
        {
          "captionCode": "common.no",
          "value": false
        }
      ],
      "optionsPerRow": 2,
      "showWhen": "!cp.specimenCentric",
      "showInOverviewWhen": "!cp.specimenCentric"
    }
  ]
}
