export default {
  fields: [
    {
      "type": "text",
      "name": "folder.name",
      "labelCode": "queries.name",
      "validations": {
        "required": {
          "messageCode": "queries.name_req"
        }
      }
    },
    {
      "type": "booleanCheckbox",
      "name": "folder.sharedWithAll",
      "inlineLabelCode": "queries.share_folder_with_all"
    },
    {
      "type": "booleanCheckbox",
      "name": "folder.allowEditsBySharedUsers",
      "inlineLabelCode": "queries.allow_edits_by_shared_users"
    },
    {
      "type": "user",
      "name": "folder.sharedWith",
      "labelCode": "queries.share_with_users",
      "multiple": true,
      "showWhen": "!folder.sharedWithAll",
      "showInOverviewWhen": "!folder.sharedWithAll"
    },
    {
      "type": "multiselect",
      "name": "folder.sharedWithGroups",
      "labelCode": "queries.share_with_user_groups",
      "listSource": { 
        "apiUrl": "user-groups",
        "displayProp": "name",
        "searchProp": "query"
      },
      "showWhen": "!folder.sharedWithAll",
      "showInOverviewWhen": "!folder.sharedWithAll"
    }
  ]
}
