
import http from '@/common/services/HttpClient.js';

class Form {
  getRecord(record, opts) {
    return http.get('forms/' + record.formId + '/data/' + record.recordId, opts);
  }

  deleteRecord(record) {
    return http.delete('forms/' + record.formId + '/data/' + record.recordId);
  }
}

export default new Form();
