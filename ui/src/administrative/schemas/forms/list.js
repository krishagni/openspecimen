import dateFmt   from '@/common/filters/DateFormatter.js';
import formSvc   from '@/forms/services/Form.js';
import routerSvc from '@/common/services/Router.js';

export default {
  key: "form.formId",

  summary: {
    title: {
      text: ({form}) => form.caption,
      url:  ({form}, query) => routerSvc.getUrl('FormsListItemDetail.Overview', {formId: form.formId}, query),
    },

    descriptions: [
      ({form}) => {
        const {firstName, lastName} =  form.createdBy || {};
        let result = firstName;
        if (lastName) {
          if (result) result += ' ';
          result += lastName;
        }

        return result || ''
      },

      ({form}) => dateFmt.dateTime(form.modificationTime || form.creationTime)
    ]
  },

  columns: [
    {
      "name": "form.caption",
      "captionCode": "forms.form_name",
      "href": ({rowObject: {form}}) => routerSvc.getUrl('FormsList', {formId: form.id})
    },
    {
      "name": "form.createdBy",
      "captionCode": "common.created_by",
      "type": "user"
    },
    {
      "name": "form.updateTime",
      "captionCode": "common.update_time",
      "value": ({form}) => dateFmt.dateTime(form.modificationTime || form.creationTime),
      "type": "date-time"
    },
    {
      "name": "form.associations",
      "captionCode": "forms.associations"
    }
  ],

  filters: [
    {
      "name": "name",
      "type": "text",
      "captionCode": "forms.form_name"
    },

    {
      "name": "cp",
      "type": "dropdown",
      "multiple": true,
      "captionCode": "forms.collection_protocol",
      "listSource": {
        "apiUrl": "collection-protocols",
        "selectProp": "shortTitle",
        "displayProp": "shortTitle",
        "searchProp": "query"
      }
    },

    {
      "name": "formType",
      "type": "dropdown",
      "captionCode": "forms.level",
      "listSource": {
        "selectProp": "entityType",
        "displayProp": "entityTypeLabel",
        "loadFn": async () => formSvc.getEntityTypes()
      }
    }
  ]
}
