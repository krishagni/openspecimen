import i18n from '@/common/services/I18n.js';

export default {
  columns: [
    {
      "name": "formContext.level.entityTypeLabel",
      "captionCode": "forms.level",
      "value": ({formContext: {formCtxtId, level}}) => '#' + formCtxtId + ' ' + level.entityTypeLabel
    },
    {
      "name": "formContext.multiRecord",
      "captionCode": "forms.multiple_records",
      "value": ({formContext}) => i18n.msg(formContext.multiRecord ? 'common.yes' : 'common.no')
    },
    {
      "name": "formContext.notifEnabled",
      "captionCode": "forms.notify_users",
      "value": ({formContext}) => i18n.msg(formContext.notifEnabled ? 'common.yes' : 'common.no')
    },
    {
      "name": "formContext.group",
      "captionCode": "forms.group"
    }
  ]
}
