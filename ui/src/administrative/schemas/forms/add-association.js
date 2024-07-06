export default {
  layout: {
    rows: [
      {
        fields: [
          {
            labelCode: 'forms.level',
            name: 'association.level',
            type: 'dropdown',
            listSource: {
              displayProp: 'entityTypeLabel',
              loadFn: ({context}) => context.formData.getEntityTypes()
            },
            validations: {
              required: {
                messageCode: 'forms.level_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            inlineLabelCode: 'forms.all_cps',
            name: 'association.allCps',
            type: 'booleanCheckbox',
            showWhen: 'association.level && association.level.cpBased && !association.level.allCps'
          }
        ]
      },
      {
        fields: [
          {
            labelCode: 'forms.collection_protocol',
            name: 'association.collectionProtocol',
            type: 'dropdown',
            listSource: {
              apiUrl: 'collection-protocols',
              displayProp: 'shortTitle'
            },
            validations: {
              required: {
                messageCode: 'forms.collection_protocol_req'
              }
            },
            showWhen: 'association.level && association.level.cpBased && !association.level.allCps && !association.allCps'
          }
        ]
      },
      {
        fields: [
          {
            inlineLabelCode: 'forms.all_institutes',
            name: 'association.allInstitutes',
            type: 'booleanCheckbox',
            showWhen: 'association.level && association.level.instituteBased && currentUser.admin'
          }
        ]
      },
      {
        fields: [
          {
            labelCode: 'forms.institute',
            name: 'association.entityId',
            type: 'dropdown',
            listSource: {
              apiUrl: 'institutes',
              displayProp: 'name',
              selectProp: 'id'
            },
            validations: {
              required: {
                messageCode: 'forms.institute_req'
              }
            },
            showWhen: 'association.level && association.level.instituteBased && !association.allInstitutes && currentUser.admin'
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
