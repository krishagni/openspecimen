import http from '@/common/services/HttpClient.js';

class QueryAudit {
  getLogs(opts) {
    return http.get('query-audit-logs', opts || {});
  }

  getLogsCount(opts) {
    return http.get('query-audit-logs/count', opts || {});
  }

  getLog(id) {
    return http.get('query-audit-logs/' + id);
  }

  exportLogs(criteria) {
    return http.post('query-audit-logs/export', criteria || {});
  }

  downloadReport(fileId) {
    http.downloadFile(http.getUrl('query-audit-logs/exported-file', {query: {fileId}}));
  }
}

export default new QueryAudit();
