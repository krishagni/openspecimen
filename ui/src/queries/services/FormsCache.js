
import http from '@/common/services/HttpClient.js';

class FormsCache {
  formsQ = null;

  fieldsQ = {};

  getForms() {
    if (!this.formsQ) { 
      this.formsQ = http.get('forms', {formType: 'Query'});
    }

    return this.formsQ;
  }

  getFields(form, cpId, cpGroupId) {
    const key = (cpId > 0 ? 'cp_' + cpId : (cpGroupId > 0 ? 'cpg_' + cpGroupId : 'none')) + '_' + form.formId;
    if (!this.fieldsQ[key]) {
      this.fieldsQ[key] = http.get('forms/' + form.formId + '/fields', {cpId, cpGroupId, extendedFields: true});
    }

    return this.fieldsQ[key];
  }

  async getField(cpId, cpGroupId, field) {
    const [formName, ...fieldNames] = field.split('.');
    if (!fieldNames || fieldNames.length == 0) {
      alert('FormCache.getField: ' + field + ' is invalid');
      return null;
    }

    const forms = await this.getForms();
    for (let form of forms) {
      if (form.name == formName) {
        const fields = await this.getFields(form, cpId, cpGroupId);
        const result = this._getField(fields, fieldNames);
        if (result == null) {
          alert('FormCache.getField: Field ' + field + ' not found');
        }

        return result;
      }
    }

    alert('FormCache.getField: Form ' + formName + ' not found');
    return null;
  }

  _getField(fields, [fieldName, ...others]) {
    for (let field of fields) {
      if (field.name == fieldName) {
        if (others.length > 0 && field.type == 'SUBFORM') {
          return this._getField(field.subFields, others);
        }

        return field;
      }
    }

    return null;
  }
}

export default new FormsCache();
