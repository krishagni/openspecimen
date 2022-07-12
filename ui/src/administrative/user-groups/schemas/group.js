
export default {
  fields: [
    {
      "type": "text",
      "labelCode": "user_groups.name",
      "name": "name",
      "validations": {
        "required": {
          "messageCode": "user_groups.name_required"
        }
      }
    },

    {
      "type": "textarea",
      "labelCode": "user_groups.description",
      "name": "description",
      "rows": 2,
      "validations": {
        "required": {
          "messageCode": "user_groups.description_required"
        }
      }
    },

    {
      "type": "dropdown",
      "labelCode": "user_groups.institute",
      "name": "institute",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name"
      },
      "validations": {
        "required": {
          "messageCode": "user_groups.institute_required"
        }
      },
      "showWhen": "showInstitute == true"
    }
  ]
}
