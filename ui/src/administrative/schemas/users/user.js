export default {
  fields: [
    {
      "type": "radio",
      "labelCode": "users.type",
      "name": "user.type",
      "optionsPerRow": 4,
      "options": (context) => {
        const options = [
          { "caption": "Super Administrator", "value": "SUPER" },
          { "caption": "Institute Administrator", "value": "INSTITUTE" },
          { "caption": "Contact", "value": "CONTACT" },
          { "caption": "Regular", "value": "NONE" }
        ];

        const currentUser = context.formData.currentUser;
        if (currentUser.instituteAdmin) {
          options.splice(0, 1);
        } else if (!currentUser.admin) {
          options.splice(0, 2);
        }

        return options;
      },
      "showWhen": "currentUser.admin || (user.type != 'SUPER' && currentUser.instituteAdmin) || (user.type != 'SUPER' && user.type != 'INSTITUTE')"
    },

    {
      "type": "text",
      "labelCode": "users.first_name",
      "name": "user.firstName",
      "validations": {
        "required": {
          "messageCode": "users.first_name_required"
        }
      }
    },

    {
      "type": "text",
      "labelCode": "users.last_name",
      "name": "user.lastName",
      "validations": {
        "required": {
          "messageCode": "users.last_name_required"
        }
      }
    },

    {
      "type": "text",
      "labelCode": "users.email_address",
      "name": "user.emailAddress",
      "validations": {
        "required": {
          "messageCode": "users.email_address_required"
        },
        "email": {
          "messageCode": "users.email_address_invalid"
        }
      }
    },

    {
      "type": "text",
      "labelCode": "users.phone_number",
      "name": "user.phoneNumber"
    },

    {
      "type": "dropdown",
      "labelCode": "users.domain_name",
      "name": "user.domainName",
      "listSource": {
        "apiUrl": "auth-domains",
        "selectProp": "name",
        "displayProp": "name"
      },
      "showWhen": "user.type != 'CONTACT'",
      "validations": {
        "required": {
          "messageCode": "users.domain_name_required"
        }
      }
    },

    {
      "type": "text",
      "labelCode": "users.login_name",
      "name": "user.loginName",
      "showWhen": "user.type != 'CONTACT'",
      "validations": {
        "required": {
          "messageCode": "users.login_name_required"
        }
      }
    },

    {
      "type": "dropdown",
      "labelCode": "users.institute",
      "name": "user.instituteName",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name"
      },
      "validations": {
        "required": {
          "messageCode": "users.institute_required"
        }
      }
    },

    {
      "type": "dropdown",
      "labelCode": "users.primary_site",
      "name": "user.primarySite",
      "showWhen": "!!user.instituteName",
      "listSource": {
        "apiUrl": "sites",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name",
        "queryParams": {
          "dynamic": {
            "institute": "user.instituteName"
          },
          "static": {
            "listAll": true
          }
        }
      }
    },

    {
      "type": "dropdown",
      "labelCode": "users.time_zone",
      "name": "user.timeZone",
      "listSource": {
        "apiUrl": "time-zones",
        "displayProp": "name",
        "selectProp": "id"
      }
    },
  
    {
      "type": "radio",
      "labelCode": "users.manage_forms",
      "name": "user.manageForms",
      "optionsPerRow": 2,
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ],
      "showWhen": "!user.type || user.type == 'NONE'"
    },

    {
      "type": "radio",
      "labelCode": "users.manage_workflows",
      "name": "user.manageWfs",
      "optionsPerRow": 2,
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ],
      "showWhen": "hasWf && (!user.type || user.type == 'NONE')",
    },

    {
      "type": "radio",
      "labelCode": "users.disable_notifs",
      "name": "user.dnd",
      "optionsPerRow": 2,
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ]
    },

    {
      "type": "radio",
      "labelCode": "users.api_user",
      "name": "user.apiUser",
      "showWhen": "user.type != 'CONTACT'",
      "optionsPerRow": 2,
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ]
    },

    {
      "type": "text",
      "labelCode": "users.ip_address",
      "name": "user.ipRange",
      "showWhen": "user.type != 'CONTACT' && user.apiUser == true",
      "validations": {
        "required": {
          "messageCode": "users.ip_address_required"
        }
      }
    },

    {
      "type": "radio",
      "labelCode": "users.download_labels",
      "name": "user.downloadLabelsPrintFile",
      "showWhen": "user.type != 'CONTACT'",
      "optionsPerRow": 2,
      "tooltip": "Download CSV files for printing labels",
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ]
    },

    {
      "type": "textarea",
      "labelCode": "users.address",
      "name": "user.address",
      "rows": 2
    }
  ]
}
