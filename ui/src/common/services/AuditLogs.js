
import http from '@/common/services/HttpClient.js';

class AuditLogs {

  async getSummary(objDetail) {
    return http.post('audit', objDetail);
  }

  async getRevisions(objDetail) {
    return http.post('audit/revisions', objDetail);
  }

}

export default new AuditLogs();
