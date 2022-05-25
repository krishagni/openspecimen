export default {
  fields:  [
    {
      "type": "text",
      "label": "Name",
      "name": "folder.name",
      "validations": {
        "required": {
          "message": "Name is mandatory"
        }
      }
    },
    {
      "type": "textarea",
      "label": "Description",
      "name": "folder.description"
    },
    {
      "type": "multiselect",
      "label": "Share with user groups",
      "name": "folder.userGroups",
      "listSource": {
        "apiUrl": "user-groups",
        "displayProp": "name",
        "searchProp": "query",
      }
    }
  ]
}
