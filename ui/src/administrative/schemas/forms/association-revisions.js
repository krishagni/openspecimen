import i18n from '@/common/services/I18n.js';

export default {
  columns: [
    {
      "name": "revision.level",
      "captionCode": "forms.level",
      "value": ({revision: {id, entityTypeLabel}}) => '#' + id + ' ' + entityTypeLabel
    },
    {
      "name": "revision.action",
      "captionCode": "common.action",
      "value": ({revision: {revType}}) => i18n.msg('common.buttons.' + ['create', 'update', 'delete'][revType])
    },
    {
      "name": "revision.revBy",
      "captionCode": "common.updated_by",
      "type": "user"
    },
    {
      "name": "revision.revTime",
      "captionCode": "common.update_time",
      "type": "date-time"
    }
  ]
}
