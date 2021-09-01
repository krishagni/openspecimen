
export default {
  fields: [
    {
      "type": "text",
      "label": "Name",
      "name": "name",
      "validations": {
        "required": {
          "message": "Name is mandatory"
        }
      }
    },

    {
      "type": "textarea",
      "label": "Description",
      "name": "description",
      "rows": 2,
      "validations": {
        "required": {
          "message": "Description is mandatory"
        }
      }
    },

    {
      "type": "dropdown",
      "label": "Institute",
      "name": "institute",
      "listSource": {
        "apiUrl": "institutes",
        "selectProp": "name",
        "displayProp": "name"
      },
      "validations": {
        "required": {
          "message": "Institute is mandatory"
        }
      },
      "showWhen": "showInstitute == true"
    }
  ]
}
