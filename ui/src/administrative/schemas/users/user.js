export default {
  fields: [
    {
      "type": "radio",
      "label": "Type",
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
      "showWhen": "user.type != 'CONTACT'",
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
      "showWhen": "user.type != 'CONTACT'",
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
      "showWhen": "!user.type || user.type == 'NONE'"
    },

    {
      "type": "radio",
      "label": "Manage Workflows?",
      "name": "user.manageWfs",
      "optionsPerRow": 2,
      "options": [
        { "caption": "Yes", "value": true },
        { "caption": "No", "value": false }
      ],
      "showWhen": "!user.type || user.type == 'NONE'"
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
      "showWhen": "user.type != 'CONTACT'",
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
      "showWhen": "user.type != 'CONTACT' && user.apiUser == true",
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
