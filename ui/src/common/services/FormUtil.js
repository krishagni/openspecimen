
import exprUtil from '@/common/services/ExpressionUtil.js';
import fieldFactory from '@/common/services/FieldFactory.js';
import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

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
        if (!field.name) {
          continue;
        }

        let ff = util.clone(field);
        ff = Object.assign(util.clone(dict[field.name] || {}), ff);
        fields.push(ff);
      }

      if (fields.length > 0) {
        result.rows.push({label: row.label, labelCode: row.labelCode, fields: fields});
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
      (row, rowIdx) => row.map(
        (field, colIdx) => {
          let fieldSchema = {
            source: 'de',
            row: rowIdx,
            column: colIdx,
            ...fieldFactory.getFieldSchema(field, namePrefix)
          };

          if (fieldSchema.type == 'subform') {
            fieldSchema.fields.forEach(field => field.source = 'de');
          }

          return fieldSchema.type && fieldSchema;
        }
      )
    ).flatMap(row => row).filter(field => !!field);
  }

  sdeFieldsToDict(fields, baseFields = []) {
    const baseFieldsMap = baseFields.reduce(
      (map, field) => {
        map[field.name] = field;
        return map;
      },
      {}
    );

    return (fields || []).map(
      (field) => {
        let result = {};
        if (field.baseField) {
          result = util.clone(baseFieldsMap[field.baseField] || {});
        }

        result = Object.assign(result, util.clone(field));
        result.label = result.label || result.caption;
        if (result.width) {
          const uiStyle = result.uiStyle = result.uiStyle || {};
          uiStyle['width'] = result.width;
        }

        if (result.type == 'textArea') {
          result.type = 'textarea';
        } else if (result.type == 'date') {
          result.type = 'datePicker';
        } else if (result.type == 'datetime') {
          result.type = 'datePicker';
          result.showTime = true;
        } else if (result.type == 'pvs') {
          result.type = 'pv';
          result.attribute = result.attr;
          result.selectProp = 'value';
        } else if (result.type == 'collection') {
          result.type = 'subform';
          result.fields = this.sdeFieldsToDict(result.fields);
        } else if (result.type == 'specimen-quantity') {
          result.entity = result.specimen || 'specimen';
        } else if (result.type == 'radio') {
          result.optionsPerRow = result.optionsPerRow || 5;
          result.options = (result.options || []).map(
            (option) => {
              if (typeof option != 'object') {
                return {caption: option, value: option};
              } else {
                return {caption: option.caption, value: option.value};
              }
            }
          );
        } else if (result.type == 'span') {
          result.displayType = result.displayType || result.formatType;
        } else if (result.type == 'file' || result.type == 'fileUpload') {
          result.type = 'fileUpload';
          result.url = http.getUrl('form-files');
          result.headers = http.headers;
        } else if (result.type == 'dropdown' && result.multiple) {
          result.type = 'multiselect';
        }

        const ls = result.listSource;
        if (ls && ls.queryParams) {
          ls.searchProp = ls.queryParams.search;
        }

        if (typeof result.showIf == 'string') {
          result.showWhen = result.showIf;
        } else if (result.showIf && typeof result.showIf == 'object') {
          let conds = (result.showIf.rules || []).map(
            (rule) => {
              if (rule.op == 'exists') {
                return '!!' + rule.field;
              } else if (rule.op == 'not_exist') {
                return '!' + rule.field;
              } else {
                return rule.field + ' ' + rule.op + ' ' + rule.value;
              }
            }
          );

          if (conds.length > 0) {
            result.showWhen = (result.showIf.op == 'OR' ? conds.join(' || ') : conds.join(' && '));
          }
        }

        result.showInOverviewWhen = result.showInOverviewIf == 'useShowIf' ? result.showWhen : result.showInOverviewIf;
        if (result.optional == false) {
          result.validations = {required: {message: result.label + ' is mandatory'}};
        }

        return result;
      }
    );
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
            field.formId = formDef.id;
            field.fqn = (namePrefix || '') + field.name;

            let fieldSchema = {source: 'de', ...fieldFactory.getFieldSchema(field, namePrefix)};
            if (fieldSchema.type) {
              rowSchema.fields.push(fieldSchema);
              if (fieldSchema.type == 'subform') {
                fieldSchema.fields.forEach(sfField => sfField.source = 'de');
              }

              if (fieldSchema.defaultValue) {
                dvRec[field.name] = fieldSchema.defaultValue;
              } else if (fieldSchema.type == 'user') {
                dvRec[field.name] = 'current_user';
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
      return object;
    }

    let valueMap = this._createCustomFieldsMap(extnDetail.attrs, useDisplayValue);
    extnDetail.attrsMap = Object.assign(valueMap, {id: extnDetail.id, containerId: extnDetail.formId});
    return object;
  }

  //
  // for now, meant only for readOnly in the Order specimens step
  //
  fromSde(fields, readOnly) {
    return fields.map(
      field => {
        field.label = field.caption;
        field.type = this._fromSdeType(field.type).type;
        field.formatType = this._fromSdeType(field.formatType).type;
        if (readOnly) {
          field.displayType = field.formatType || field.type;
          field.type = 'span';
        }

        const uiStyle = field.uiStyle = field.uiStyle || {};
        if (field.width) {
          uiStyle['min-width'] = field.width;
        }

        return field;
      }
    );
  }

  setDefaultValues(formSchema, formData, setOnlyIfEmpty) {
    formSchema = formSchema || {};
    formSchema.rows = formSchema.rows || [];
    for (let {fields} of formSchema.rows) {
      for (let field of fields) {
        if (field.type == 'subform' || field.defaultValue == undefined || field.defaultValue == null) {
          continue;
        }

        if (setOnlyIfEmpty) {
          const existingValue = exprUtil.eval(formData, field.name);
          if (existingValue != null && existingValue != undefined && existingValue != '') {
            continue;
          }
        }

        let value = field.defaultValue;
        if (value == 'current_date') {
          value = new Date();
        } else if (value == 'current_user') {
          if (field.name.indexOf('extensionDetail') != -1) {
            value = window.osUi.currentUser.id;
          } else {
            value = window.osUi.currentUser;
          }
        } else if (typeof value == 'string' && value.indexOf('field:') == 0) {
          const accessor = value.substring('field:'.length).trim();
          value = exprUtil.eval(formData, accessor);
        }

        exprUtil.setValue(formData, field.name, value);
      }
    }
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
      if (!useDisplayValue && attr.displayValue) {
        valueMap[attr.name + '$displayValue'] = attr.displayValue;
      }
    }

    return valueMap;
  }

  _fromSdeType(type) {
    switch (type) {
      case 'pvs':
        return {type: 'pv'};

      case 'specimen-quantity':
        return {type: 'specimen-measure'};

    }

    return {type};
  }
}

export default new FormUtil();
