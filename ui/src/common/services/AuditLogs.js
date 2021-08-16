
import httpSvc from '@/common/services/HttpClient.js';

class AuditLogs {

  async getSummary(objDetail) {
    return httpSvc.post('audit', objDetail);
  }

}

export default new AuditLogs();
