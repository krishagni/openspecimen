
import * as Validators from '@vuelidate/validators';
import { requiredIf } from '@vuelidate/validators';

import http from '@/common/services/HttpClient.js';
import formSvc from '@/forms/services/Form.js';
import exprUtil from '@/common/services/ExpressionUtil.js';

class FieldFactory {

  fieldTypes = {
    dropdown: 'os-dropdown',
    multiselect: 'os-multi-select-dropdown',
    groupselect: 'os-multi-select-group-dropdown',
    radio: 'os-radio-button',
    checkbox: 'os-checkbox',
    booleanCheckbox: 'os-boolean-checkbox',
    'toggle-checkbox': 'os-boolean-checkbox',
    text: 'os-input-text',
    textarea: 'os-textarea',
    password: 'os-password',
    number: 'os-input-number',
    datePicker: 'os-date-picker',
    fileUpload: 'os-file-upload',
    signature: 'os-signature-pad',
    user: 'os-user-dropdown',
    pv: 'os-pv-dropdown',
    site: 'os-site-dropdown',
    span: 'os-span',
    'specimen-measure': 'os-specimen-measure',
    'specimen-quantity': 'os-specimen-measure',
    'specimen-type': 'os-specimen-type',
    storageContainer: 'os-containers-dropdown',
    subform: 'os-subform',
    'storage-position': 'os-storage-position',
    note: 'os-note',
    'add-specimens': 'os-add-specimens',
    'specimen-description': 'os-specimen-description'
  };

  getComponent(fieldType) {
    return this.fieldTypes[fieldType] || 'os-unknown';
  }

  registerComponent(fieldType, componentName) {
    this.fieldTypes[fieldType] = componentName;
  }

  getValidationRules(fields) {
    let rules = {};

    for (let field of fields) {
      if (!Object.prototype.hasOwnProperty.call(field, 'validations')) {
        continue;
      }

      let validations = {};
      for (let rule in field.validations) {
        let fv = field.validations[rule];
        if (rule == 'requiredIf') {
          /*validations[rule] = (value, form) => {
            const required = exprUtil.eval({...form.$context, ...form}, fv.expr);
            return !required || (value == 0 || !!value);
          }*/

          validations[rule] = requiredIf(new Function('return ' + exprUtil.parse(fv.expr)));
        } else if (rule == 'required' && field.showWhen) {
          validations[rule] = requiredIf(new Function('return ' + exprUtil.parse(field.showWhen)));
        } else if (rule == 'pattern') {
          validations[rule] = (value) => this._isEmpty(value) || this._matches(fv.expr, value);
        } else if (rule == 'sameAs') {
          validations[rule] = (value, form) => form[fv.field] == value
        } else if (rule == 'lt' || rule == 'le' || rule == 'gt' || rule == 'ge') {
          validations[rule] = (value, form) => {
            if (value == undefined || value == null) {
              return true;
            }

            let target = fv.value;
            if ((target == undefined || target == null) && fv.expr) {
              target = form[fv.expr]
              if (target == undefined || target == null) {
                target = exprUtil.eval(form.$context, fv.expr);
              }
            }

            if (target == undefined || target == null) {
              return false;
            }

            if (rule == 'lt') {
              return value < target;
            } else if (rule == 'le') {
              return value <= target;
            } else if (rule == 'gt') {
              return value > target;
            } else if (rule == 'ge') {
              return value >= target;
            }

            return false;
          }
        } else if (rule == 'nz') {
          validations[rule] = (value) => value != 0
        } else if (fv.params) {
          validations[rule] = Validators[rule](fv.params);
        } else {
          validations[rule] = Validators[rule]
        }
      }

      rules[field.name] = validations;
    }

    return rules;
  }

  getFieldSchema(field, namePrefix) {
    let fs = {
      name: (namePrefix || '') + field.name,
      label: field.caption,
      tooltip: field.toolTip,
      showWhen: field.showWhen
    };

    if (field.type == 'stringTextField') {
      fs.type = 'text';
      fs.defaultValue = field.defaultValue;
    } else if (field.type == 'textArea') {
      fs.type = 'textarea';
      fs.rows = field.noOfRows;
      fs.defaultValue = field.defaultValue;
    } else if (field.type == 'numberField') {
      fs.type = 'number';
      fs.maxFractionDigits = field.noOfDigitsAfterDecimal || 0;
      fs.defaultValue = field.defaultValue;
    } else if (field.type == 'radiobutton') {
      fs.type = 'radio';
      fs.options = (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value}));
      fs.optionsPerRow = field.optionsPerRow || 1;
      fs.defaultValue = this.getDefaultOption(field);
    } else if (field.type == 'checkbox') {
      fs.type = 'checkbox';
      fs.options = (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value}));
      fs.optionsPerRow = field.optionsPerRow || 1;
      fs.defaultValue = this.getDefaultOption(field);
      if (fs.defaultValue) {
        fs.defaultValue = [fs.defaultValue];
      }
    } else if (field.type == 'booleanCheckbox') {
      fs.type = 'booleanCheckbox';
      if (field.defaultChecked) {
        fs.defaultValue = true;
      }
    } else if (field.type == 'combobox') {
      fs.type = 'dropdown';
      fs.listSource = this.getListSource(field);
      fs.defaultValue = this.getDefaultOption(field);
    } else if (field.type == 'multiSelectListbox') {
      fs.type = 'multiselect';
      fs.listSource = this.getListSource(field);
      fs.defaultValue = this.getDefaultOption(field);
      if (fs.defaultValue) {
        fs.defaultValue = [fs.defaultValue];
      }
    } else if (field.type == 'datePicker') {
      fs.type = 'datePicker';
      fs.showTime = field.format && field.format.indexOf('HH:mm') > 0;
      if (field.defaultType == 'CURRENT_DATE' || field.defaultValue == 'current_date') {
        fs.defaultValue = '' + new Date().getTime();
      }
    } else if (field.type == 'fileUpload') {
      fs.type = 'fileUpload';
      fs.url = http.getUrl('form-files');
      fs.headers = http.headers;
    } else if (field.type == 'signature') {
      fs.type = 'signature';
      fs.uploader = (data) => http.post('form-files/images', {dataUrl: data}).then((r) => r.fileId);
      fs.imageUrl = (fileId) => http.getUrl('form-files/' + fileId);
    } else if (field.type == 'userField') {
      fs.type = 'user';
      fs.selectProp = 'id';
    } else if (field.type == 'pvField') {
      fs.type = 'pv';
      fs.selectProp = 'value';
      fs.attribute = field.attribute;
      fs.leafValue = field.leafValue;
    } else if (field.type == 'siteField') {
      fs.type = 'site';
      fs.selectProp = 'id';
    } else if (field.type == 'storageContainer') {
      fs.type = 'storageContainer';
      fs.selectProp = 'id';
    } else if (field.type == 'subForm') {
      fs.type = 'subform';
      let fields = fs.fields = [];
      for (let sr of field.rows) {
        for (let subfield of sr) {
          fields.push(this.getFieldSchema(subfield));
        }
      }
    } else if (field.type == 'label') {
      fs.type = 'note';
      fs.note = field.caption || field.label;
      fs.label = '';
    }

    if (fs.type) {
      fs.component = this.getComponent(fs.type);
    }

    fs.validations = fs.validations = {};
    if (field.mandatory == true) {
      fs.validations.required = {message: field.caption + ' is mandatory'};
    }

    if (field.validationRules) {
      for (let rule of field.validationRules) {
        if (rule.name == 'range') {
          if (rule.params && rule.params.min != undefined && rule.params.min != null) {
            fs.validations.minValue = {
              params: rule.params.min,
              message: field.caption + ' is less than ' + rule.params.min
            }
          }

          if (rule.params && rule.params.max != undefined && rule.params.max != null) {
            fs.validations.maxValue = {
              params: rule.params.max,
              message: field.caption + ' is greater than ' + rule.params.max
            }
          }
        }
      }
    }

    return fs;
  }

  getDefaultOption(field) {
    if (typeof field.defaultValue == 'object') {
      return field.defaultValue.optionName || field.defaultValue.value;
    } else if (typeof field.defaultValue == 'string') {
      return field.defaultValue;
    }

    return undefined;
  }

  getListSource(field) {
    let ls = {
      options: (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value})),
      displayProp: 'caption',
      selectProp: 'value'
    }

    let defOptions = ls.options;
    let cachedPvs = {};
    if (field.pvs && field.pvs.length >= 100) {
      ls.searchProp = 'query';
      ls.options = undefined;
      ls.loadFn = async function({query}) {
        if (!query) {
          return defOptions;
        } else {
          let pvs = cachedPvs[query];
          if (!pvs) {
            pvs = cachedPvs[query] = await formSvc.getPvs(field.formId, field.fqn, query);
          }
          return pvs.map(pv => ({caption: pv.optionName || pv.value, value: pv.value}));
        }
      }
    }

    return ls;
  }

  _isEmpty(input) {
    return input == null || input == undefined || (typeof input == 'string' && input.trim().length == 0);
  }

  _matches(pattern, input) {
    return new RegExp(pattern).test(input);
  }
}

export default new FieldFactory();
