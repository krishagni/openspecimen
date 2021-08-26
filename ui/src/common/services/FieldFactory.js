
import * as Validators from '@vuelidate/validators';
import { requiredIf } from '@vuelidate/validators';

import http from '@/common/services/HttpClient.js';

class FieldFactory {

  fieldTypes = {
    dropdown: 'Dropdown',
    multiselect: 'MultiSelectDropdown',
    radio: 'RadioButton',
    checkbox: 'Checkbox',
    booleanCheckbox: 'BooleanCheckbox',
    text: 'InputText',
    textarea: 'Textarea',
    password: 'Password',
    number: 'InputNumber',
    datePicker: 'DatePicker',
    fileUpload: 'FileUpload',
    signature: 'SignaturePad',
    user: 'UserDropdown',
    pv: 'PvDropdown',
    site: 'SiteDropdown',
    storageContainer: 'StorageContainerDropdown',
    subform: 'Subform'
  };

  getComponent(fieldType) {
    let component = this.fieldTypes[fieldType];
    if (!component) {
      component = 'Unknown Component';
    }

    return component;
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
          validations[rule] = requiredIf(new Function('return ' + fv.expr));
        } else if (field.showWhen) {
          validations[rule] = requiredIf(new Function('return ' + field.showWhen));
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
    let fs = { name: field.udn, label: field.caption };
    if (field.type == 'stringTextField') {
      fs.type = 'text';
    } else if (field.type == 'textArea') {
      fs.type = 'textarea';
    } else if (field.type == 'numberField') {
      fs.type = 'number';
      fs.maxFractionDigits = field.noOfDigitsAfterDecimal || 0;
    } else if (field.type == 'radiobutton') {
      fs.type = 'radio';
      fs.options = (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value}));
      fs.optionsPerRow = field.optionsPerRow || 1;
    } else if (field.type == 'checkbox') {
      fs.type = 'checkbox';
      fs.options = (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value}));
      fs.optionsPerRow = field.optionsPerRow || 1;
    } else if (field.type == 'booleanCheckbox') {
      fs.type = 'booleanCheckbox';
    } else if (field.type == 'combobox') {
      fs.type = 'dropdown';
      fs.listSource = {
        options: (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value})),
        displayProp: 'caption',
        selectProp: 'value'
      }
    } else if (field.type == 'multiSelectListbox') {
      fs.type = 'multiselect';
      fs.listSource = {
        options: (field.pvs || []).map((pv) => ({caption: pv.optionName || pv.value, value: pv.value})),
        displayProp: 'caption',
        selectProp: 'value'
      }
    } else if (field.type == 'datePicker') {
      fs.type = 'datePicker';
      fs.showTime = field.format && field.format.indexOf('HH:mm') > 0;
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
}

export default new FieldFactory();
