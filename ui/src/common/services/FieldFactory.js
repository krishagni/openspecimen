
import * as Validators from '@vuelidate/validators';
import { requiredIf } from '@vuelidate/validators';

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
    datePicker: 'DatePicker',
    fileUpload: 'FileUpload',
    'signature': 'SignaturePad'
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
}

export default new FieldFactory();
