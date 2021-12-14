export default {
  fields: [
    {
      "type": "radio",
      "label": "Type",
      "name": "user.type",
      "optionsPerRow": 4,
      "options": (context) => {
        const currentUser = context.formData.currentUser;
        if (!currentUser.admin && !currentUser.instituteAdmin) {
          return [];
        }

        const options = [
          { "caption": "Super Administrator", "value": "SUPER" },
          { "caption": "Institute Administrator", "value": "INSTITUTE" },
          { "caption": "Contact", "value": "CONTACT" },
          { "caption": "Regular", "value": "NONE" }
        ];

        if (currentUser.instituteAdmin) {
          options.splice(0, 1);
        }

        return options;
      },
      "showWhen": "currentUser.admin || (user.type != 'SUPER' && currentUser.instituteAdmin)"
    },

    {
      "type": "text",
      "label": "First Name",
      "name": "user.firstName",
      "validations": {
        "required": {
          "message": "First name is mandatory"
        }
      }
    },

    {
      "type": "text",
      "label": "Last Name",
      "name": "user.lastName",
      "validations": {
        "required": {
          "message": "Last name is mandatory"
        }
      }
    },

    {
      "type": "text",
      "label": "Email Address",
      "name": "user.emailAddress",
      "validations": {
        "required": {
          "message": "Email address is mandatory"
        },
        "email": {
          "message": "Email address is invalid"
        }
      }
    },

    {
      "type": "text",
      "label": "Phone Number",
      "name": "user.phoneNumber"
    },

    {
      "type": "dropdown",
      "label": "Domain Name",
      "name": "user.domainName",
      "listSource": {
        "apiUrl": "auth-domains",
        "selectProp": "name",
        "displayProp": "name"
      },
      "showWhen": "type != 'CONTACT'",
      "validations": {
        "required": {
          "message": "Domain name is mandatory"
        }
      }
    },

    {
      "type": "text",
      "label": "Login Name",
      "name": "user.loginName",
      "showWhen": "type != 'CONTACT'",
      "validations": {
        "required": {
          "message": "Login name is mandatory"
        }
      }
    },

    {
      "type": "dropdown",
      "label": "Institute",
      "name": "user.instituteName",
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
      }
    },

    {
      "type": "dropdown",
      "label": "Primary Site",
      "name": "user.primarySite",
      "showWhen": "!!instituteName",
      "listSource": {
        "apiUrl": "sites",
        "selectProp": "name",
        "displayProp": "name",
        "searchProp": "name",
        "queryParams": {
          "dynamic": {
            "institute": "instituteName"
          }
        }
      }
    },

    {
      "type": "dropdown",
      "label": "Time Zone",
      "name": "user.timeZone",
      "listSource": {
        "apiUrl": "time-zones",
        "displayProp": "name",
        "selectProp": "id"
      }
    },
  
    {
      "type": "radio",
      "label": "Manage Forms?",
      "name": "user.manageForms",
      "optionsPerRow": 2,
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ],
      "showWhen": "!type || type == 'NONE'"
    },

    {
      "type": "radio",
      "label": "Disable Notifications?",
      "name": "user.dnd",
      "optionsPerRow": 2,
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ]
    },

    {
      "type": "radio",
      "label": "API User?",
      "name": "user.apiUser",
      "showWhen": "type != 'CONTACT'",
      "optionsPerRow": 2,
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ]
    },

    {
      "type": "text",
      "label": "IP Address",
      "name": "user.ipRange",
      "showWhen": "type != 'CONTACT' && apiUser == true",
      "validations": {
        "required": {
          "message": "IP address is mandatory"
        }
      }
    },

    {
      "type": "textarea",
      "label": "Address",
      "name": "user.address",
      "rows": 2
    }
  ]
}