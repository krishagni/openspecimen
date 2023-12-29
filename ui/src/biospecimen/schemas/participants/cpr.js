export default {
  fields:  [
    {
      "type": "datePicker",
      "labelCode": "participants.registration_date",
      "name": "cpr.registrationDate",
      "validations": {
        "required": {
          "messageCode": "participants.registration_date_req"
        }
      }
    },
    {
      "type": "text",
      "labelCode": "participants.ppid",
      "name": "cpr.ppid",
      "validations": {
        "required": {
          "messageCode": "participants.ppid_req"
        }
      },
      "showWhen": "!!cpr.ppid || !cp.ppidFmt || cp.manualPpidEnabled",
      "disableWhen": "!!cpr.ppid"
    },
    { 
      "type": "site",
      "labelCode": "participants.registration_site",
      "name": "cpr.site",
      "selectProp": "name",
      "listSource": {
        "selectProp": "name"
      }
    },
    {
      "type": "text",
      "labelCode": "participants.external_subject_id",
      "name": "cpr.externalSubjectId"
    },
    {
      "type": "text",
      "labelCode": "participants.first_name",
      "name": "cpr.participant.firstName"
    },
    {
      "type": "text",
      "labelCode": "participants.middle_name",
      "name": "cpr.participant.middleName"
    },
    {
      "type": "text",
      "labelCode": "participants.last_name",
      "name": "cpr.participant.lastName"
    },
    {
      "type": "text",
      "labelCode": "participants.email_address",
      "name": "cpr.participant.emailAddress"
    },
    {
      "type": "booleanCheckbox",
      "inlineLabelCode": "participants.receive_emails",
      "name": "cpr.participant.emailOptIn",
      "displayValues": {
        true: window.osSvc.i18nSvc.msg('common.yes'),
        false: window.osSvc.i18nSvc.msg('common.no')
      },
      "showWhen": "!!cpr.participant.emailAddress"
    },
    {
      "type": "text",
      "labelCode": "participants.phone_number",
      "name": "cpr.participant.phoneNumber"
    },
    {
      "type": "booleanCheckbox",
      "inlineLabelCode": "participants.receive_smses",
      "name": "cpr.participant.textOptIn",
      "displayValues": {
        true: window.osSvc.i18nSvc.msg('common.yes'),
        false: window.osSvc.i18nSvc.msg('common.no')
      },
      "showWhen": "!!cpr.participant.phoneNumber"
    },
    {
      "type": "datePicker",
      "labelCode": "participants.birth_date",
      "name": "cpr.participant.birthDate"
    },
    {
      "type": "text",
      "labelCode": "participants.empi",
      "name": "cpr.participant.empi"
    },
    {
      "type": "text",
      "labelCode": "participants.uid",
      "name": "cpr.participant.uid"
    },
    {
      "type": "pv",
      "labelCode": "participants.gender",
      "name": "cpr.participant.gender",
      "attribute": "gender",
      "selectProp": "value"
    },
    {
      "type": "pv",
      "labelCode": "participants.vital_status",
      "name": "cpr.participant.vitalStatus",
      "attribute": "vital_status",
      "selectProp": "value"
    },
    {
      "type": "datePicker",
      "labelCode": "participants.death_date",
      "name": "cpr.participant.deathDate",
      "showInOverviewWhen": "!!cpr.participant.deathDate",
      "showWhen": "cpr.participant.dead"
    },
    {
      "type": "pv",
      "labelCode": "participants.races",
      "name": "cpr.participant.races",
      "multiple": true,
      "attribute": "race",
      "selectProp": "value"
    },
    {
      "type": "pv",
      "labelCode": "participants.ethnicities",
      "name": "cpr.participant.ethnicities",
      "multiple": true,
      "attribute": "ethnicity",
      "selectProp": "value"
    },
    {
      "type": "subform",
      "labelCode": "participants.medical_ids",
      "name": "cpr.participant.pmis",
      "showInOverviewWhen": "cpr.participant.pmis && cpr.participant.pmis.length > 0",
      "fields": [
        {
          "type": "site",
          "labelCode": "participants.site",
          "name": "siteName",
          "selectProp": "name",
          "listSource": {
            "selectProp": "name"
          },
          "validations": {
            "requiredIf": {
              "expr": "!!mrn",
              "messageCode": "participants.site_req"
            }
          }
        },
        {
          "type": "text",
          "labelCode": "participants.mrn",
          "name": "mrn"
        }
      ]
    }
  ]
}
