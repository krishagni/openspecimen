export default {
  fields:  [
    {
      "type": "text",
      "label": "Name",
      "name": "cart.name",
      "validations": {
        "required": {
          "message": "Name is mandatory"
        }
      },
      "showWhen": "!defaultCart",
      "dataEntry": true
    },
    {
      "type": "user",
      "label": "Share with users",
      "name": "cart.sharedWith",
      "multiple": true,
      "listSource": {
        "queryParams": {
          "static": {
            "excludeType": "CONTACT"
          }
        }
      }
    },
    {
      "type": "multiselect",
      "label": "Share with user groups",
      "name": "cart.sharedWithGroups",
      "listSource": {
        "apiUrl": "user-groups",
        "displayProp": "name",
        "searchProp": "query",
      }
    },
    {
      "type": "textarea",
      "label": "Description",
      "name": "cart.description"
    },
    {
      "type": "add-specimens",
      "label": "Specimens",
      "name": "cart.specimenLabels",
      "hideButtons": true,
      "placeholder": "Add specimens to the cart by scanning labels or barcodes separated by comma, tab, or newline",
      "validations": {
        "requiredIf": {
          "expr": "!cart.id && noInputSpecimens",
          "message": "Specimen labels is mandatory"
        }
      },
      "showWhen": "noInputSpecimens",
      "dataEntry": true
    }
  ]
}
