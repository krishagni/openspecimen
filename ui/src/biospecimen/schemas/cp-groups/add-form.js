export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "formCtx.form",
            type: "dropdown",
            labelCode: "cpgs.form",
            listSource: {
              displayProp: "caption",
              loadFn: ({context, query}) => context.formData.getForms(query)
            },
            validations: {
              required: {
                messageCode: "cpgs.form_req"
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            name: "formCtx.multipleRecords",
            type: "booleanCheckbox",
            inlineLabelCode: "cpgs.multiple_records",
            showWhen: "!formCtx.customFields"
          }
        ]
      },
      {
        fields: [
          {
            name: "formCtx.notifEnabled",
            type: "booleanCheckbox",
            inlineLabelCode: "cpgs.form_notifs_enabled",
            showWhen: "!formCtx.customFields"
          }
        ]
      },
      {
        fields: [
          {
            name: "formCtx.dataInNotif",
            type: "booleanCheckbox",
            inlineLabelCode: "cpgs.form_data_in_notif",
            showWhen: "!formCtx.customFields && formCtx.notifEnabled"
          }
        ]
      },
      {
        fields: [
          {
            name: "formCtx.notifUserGroups",
            type: "multiselect",
            labelCode: "cpgs.notif_rcpts",
            showWhen: "!formCtx.customFields && formCtx.notifEnabled",
            listSource: {
              apiUrl: "user-groups",
              displayProp: "name",
              searchProp: "query"
            },
            validations: {
              required: {
                messageCode: "cpgs.notif_rcpts_req"
              }
            }
          }
        ]
      }
    ]
  }
}
