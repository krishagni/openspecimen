export default {
  fields:  [
    {
      "type": "text",
      "labelCode": "carts.name",
      "name": "folder.name",
      "validations": {
        "required": {
          "messageCode": "carts.name_required"
        }
      }
    },
    {
      "type": "textarea",
      "labelCode": "carts.description",
      "name": "folder.description"
    },
    {
      "type": "multiselect",
      "labelCode": "carts.share_with_user_groups",
      "name": "folder.userGroups",
      "listSource": {
        "apiUrl": "user-groups",
        "displayProp": "name",
        "searchProp": "query",
      }
    }
  ]
}
