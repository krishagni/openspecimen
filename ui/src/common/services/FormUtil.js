
import fieldFactory from '@/common/services/FieldFactory.js';

class FormUtil {

  getFormSchema(baseSchema, layoutSchema) {
    if (!layoutSchema.rows || layoutSchema.rows.length == 0) {
      return [];
    }

    let dict = baseSchema.reduce((acc, field) => (acc[field.name] = field) && acc, {});
    let result = {rows: []};
    for (let row of layoutSchema.rows) {
      let fields = [];
      for (let field of row.fields) {
        let ff = JSON.parse(JSON.stringify(field));
        if (!ff.name || !dict[ff.name]) {
          continue;
        }

        ff = Object.assign(JSON.parse(JSON.stringify(dict[ff.name])), ff);
        fields.push(ff);
      }

      if (fields.length > 0) {
        result.rows.push({fields: fields});      
      }
    }

    return result;
  }

  relinkFormRecords(forms, records) {
    let fcMap = {};
    forms.forEach((form) => { form.records = []; fcMap[form.formCtxtId] = form });
    records.forEach(
      (formRecs) => {
        formRecs.records.forEach(
          (record) => {
            let form = fcMap[record.fcId];
            form.records = form.records || [];
            form.records.push(record);
          }
        );
      }
    );
  }

  deFormToDict(formDef, namePrefix) {
    if (!formDef || !formDef.rows) {
      return [];
    }

    return formDef.rows.map(
      (row) => row.map(
        (field) => {
          let fieldSchema = {source: 'de', ...fieldFactory.getFieldSchema(field, namePrefix)};
          if (fieldSchema.type == 'subform') {
            fieldSchema.fields.forEach(field => field.source = 'de');
          }

          return fieldSchema.type && fieldSchema;
        }
      )
    ).flatMap(row => row).filter(field => !!field);
  }

  fromDeToStdSchema(formDef, namePrefix) {
    let schema = { rows: [] };
    let dvRec = {}; // default values record

    if (!formDef || !formDef.rows) {
      return { schema, defaultValues: dvRec};
    }

    formDef.rows.forEach(
      (row) => {
        let rowSchema = {fields: []};
        row.forEach(
          (field) => {
            let fieldSchema = fieldFactory.getFieldSchema(field, namePrefix);
            if (fieldSchema.type) {
              rowSchema.fields.push(fieldSchema);
              if (fieldSchema.defaultValue) {
                dvRec[field.name] = fieldSchema.defaultValue;
              }
            }
          }
        );
        schema.rows.push(rowSchema);
      }
    );

    return { schema, defaultValues: dvRec};
  }

  createCustomFieldsMap(object, useDisplayValue) {
    let extnDetail = object.extensionDetail;
    if (!extnDetail || !extnDetail.attrs) {
      return;
    }

    let valueMap = this._createCustomFieldsMap(extnDetail.attrs, useDisplayValue);
    extnDetail.attrsMap = Object.assign(valueMap, {id: extnDetail.id, containerId: extnDetail.formId});
  }

  _createCustomFieldsMap(attrs, useDisplayValue) {
    let valueMap = {};

    for (let attr of attrs) {
      let value = attr.value;
      if (attr.type == 'subForm') {
        value = (attr.value || []).map(sfAttrs => this._createCustomFieldsMap(sfAttrs, useDisplayValue));
      } else if (attr.type != 'fileUpload' && useDisplayValue && attr.displayValue) {
        value = attr.displayValue;
      } else if (attr.type == 'datePicker') {
        if (!isNaN(attr.value) && !isNaN(parseInt(attr.value))) {
          value = new Date(parseInt(attr.value));
        } else if (!!attr.value || attr.value === 0) {
          value = new Date(attr.value);
        }
      }

      valueMap[attr.name] = value;
    }

    return valueMap;
  }
}

export default new FormUtil();
