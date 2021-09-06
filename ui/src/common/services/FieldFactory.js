
import * as Validators from '@vuelidate/validators';
import { requiredIf } from '@vuelidate/validators';

import http from '@/common/services/HttpClient.js';
import exprUtil from '@/common/services/ExpressionUtil.js';

class FieldFactory {

  fieldTypes = {
    dropdown: 'os-dropdown',
    multiselect: 'os-multi-select-dropdown',
    radio: 'os-radio-button',
    checkbox: 'os-checkbox',
    booleanCheckbox: 'os-boolean-checkbox',
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
    storageContainer: 'os-containers-dropdown',
    subform: 'os-subform'
  };

  getComponent(fieldType) {
    return this.fieldTypes[fieldType] || 'Unknown Component';
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
          validations[rule] = requiredIf(new Function('return ' + exprUtil.parse(fv.expr)));
        } else if (rule == 'required' && field.showWhen) {
          validations[rule] = requiredIf(new Function('return ' + exprUtil.parse(field.showWhen)));
        } else if (rule == 'pattern') {
          validations[rule] = (value) => new RegExp(fv.expr).test(value);
        } else if (rule == 'sameAs') {
          validations[rule] = (value, form) => form.fd(fv.field) == value;
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

  getFieldSchema(field) {
    let fs = {
      name: field.udn,
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
      fs.listSource = {
        options: (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value})),
        displayProp: 'caption',
        selectProp: 'value'
      }
      fs.defaultValue = this.getDefaultOption(field);
    } else if (field.type == 'multiSelectListbox') {
      fs.type = 'multiselect';
      fs.listSource = {
        options: (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value})),
        displayProp: 'caption',
        selectProp: 'value'
      }
      fs.defaultValue = this.getDefaultOption(field);
      if (fs.defaultValue) {
        fs.defaultValue = [fs.defaultValue];
      }
    } else if (field.type == 'datePicker') {
      fs.type = 'datePicker';
      fs.showTime = field.format && field.format.indexOf('HH:mm') > 0;
      if (field.defaultType == 'CURRENT_DATE') {
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
      fs.selectProp = 'id';
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
}

export default new FieldFactory();
