export default {
  layout: {
    rows: [
      {
        fields: [
          {
            labelCode: 'forms.level',
            name: 'association.level.entityTypeLabel',
            type: 'span'
          }
        ]
      },
      {
        fields: [
          {
            labelCode: 'forms.collection_protocol',
            name: 'association.group',
            type: 'span',
            showWhen: 'association.level && association.level.cpBased'
          }
        ]
      },
      {
        fields: [
          {
            labelCode: 'forms.institute',
            name: 'association.group',
            type: 'span',
            showWhen: 'association.level && association.level.instituteBased'
          }
        ]
      },
      {
        fields: [
          {
            inlineLabelCode: 'forms.multiple_records',
            name: 'association.multiRecord',
            type: 'booleanCheckbox',
            showWhen: 'association.level && association.level.allowMultipleRecs'
          }
        ]
      },
      {
        fields: [
          {
            inlineLabelCode: 'forms.send_notif_to_users',
            name: 'association.notifEnabled',
            type: 'booleanCheckbox',
            showWhen: 'association.level && association.level.allowNotifs'
          }
        ]
      },
      {
        fields: [
          {
            inlineLabelCode: 'forms.incl_data_in_notifs',
            name: 'association.dataInNotif',
            type: 'booleanCheckbox',
            showWhen: 'association.notifEnabled'
          }
        ]
      },
      {
        fields: [
          {
            labelCode: 'forms.notify_users',
            name: 'association.notifUserGroups',
            type: 'multiselect',
            listSource: {
              apiUrl: 'user-groups',
              displayProp: 'name'
            },
            showWhen: 'association.notifEnabled'
          }
        ]
      }
    ]
  }
}
