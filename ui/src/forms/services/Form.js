
import http from '@/common/services/HttpClient.js';

class Form {
  async getDefinition(formId) {
    const formDef = await http.get('forms/' + formId + '/definition', {maxPvs: 100});
    return this._addFormId(formDef);
  }

  async getDefinitionByName(formName) {
    const formDef = await http.get('forms/definition', {name: formName, maxPvs: 100});
    return this._addFormId(formDef);
  }

  async getPvs(formId, fieldName, query) {
    let params = {controlName: fieldName};
    if (query) {
      params.searchString = query;
    }

    return http.get('forms/' + formId + '/permissible-values', params);
  }

  async getRecord(record, opts) {
    return http.get('forms/' + record.formId + '/data/' + record.recordId, opts);
  }

  async saveOrUpdateRecord(record) {
    record.appData = record.appData || {};
    let formId = record.appData.formId;
    if (!formId) {
      alert('Unknown form ID. Maybe a bug in the UI contact. Contact the system administrator');
      alert(JSON.stringify(record));
      return;
    }

    if (record.id) {
      return http.put('forms/' + formId + '/data', record);
    } else {
      return http.post('forms/' + formId + '/data', record);
    }
  }

  async deleteRecord(record) {
    return http.delete('forms/' + record.formId + '/data/' + record.recordId);
  }

  _addFormId(formDef) {
    formDef.rows.forEach(
      row => row.forEach(
        (field) => {
          field.formId = formDef.id;
          if (field.type == 'subForm') {
            field.rows.forEach(
              sfRow => sfRow.forEach(
                sfField => {
                  sfField.formId = formDef.id;
                  sfField.fqn = field.name + '.' + sfField.name;
                }
              )
            );
          } else {
            field.fqn = field.name;
          }
        }
      )
    );

    return formDef;
  }
}

export default new Form();
