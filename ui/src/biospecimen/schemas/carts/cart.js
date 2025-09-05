export default {
  fields:  [
    {
      "type": "text",
      "labelCode": "carts.name",
      "name": "cart.name",
      "validations": {
        "required": {
          "messageCode": "carts.name_required"
        }
      },
      "showWhen": "!defaultCart",
      "dataEntry": true
    },
    {
      "type": "user",
      "labelCode": "carts.share_with_users",
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
      "labelCode": "carts.share_with_user_groups",
      "name": "cart.sharedWithGroups",
      "listSource": {
        "apiUrl": "user-groups",
        "displayProp": "name",
        "searchProp": "query",
        "queryParams": {
          "static": {
            "listAll": true
          }
        }
      }
    },
    {
      "type": "textarea",
      "labelCode": "carts.description",
      "name": "cart.description"
    },
    {
      "type": "add-specimens",
      "labelCode": "carts.specimens",
      "name": "cart.specimenLabels",
      "hideAddButton": true,
      "placeholder": "Add specimens to the cart by scanning labels or barcodes separated by comma, tab, or newline",
      "showWhen": "noInputSpecimens",
      "dataEntry": true
    },
    {
      "type": "booleanCheckbox",
      "inlineLabelCode": "carts.send_digest_notifications",
      "name": "cart.sendDigestNotifs"
    }
  ]
}
