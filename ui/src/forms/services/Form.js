
import http from '@/common/services/HttpClient.js';

class Form {
  deleteRecord(record) {
    return http.delete('forms/' + record.formId + '/data/' + record.recordId);
  }
}

export default new Form();
