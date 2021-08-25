
import http from '@/common/services/HttpClient.js';

class Form {
  async getDefinition(formId) {
    return http.get('forms/' + formId + '/definition');
  }

  async getRecord(record, opts) {
    return http.get('forms/' + record.formId + '/data/' + record.recordId, opts);
  }

  async saveOrUpdateRecord(record) {
    record.appData = record.appData || {};
    let formId = record.appData.formId;
    if (record.id) {
      return http.put('forms/' + formId + '/data', record);
    } else {
      return http.post('forms/' + formId + '/data', record);
    }
  }

  async deleteRecord(record) {
    return http.delete('forms/' + record.formId + '/data/' + record.recordId);
  }
}

export default new Form();
