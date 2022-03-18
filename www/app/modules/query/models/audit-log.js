
angular.module('os.query.models.auditlog', ['os.common.models'])
  .factory('QueryAuditLog', function($http, osModel, QueryUtil) {
    var QueryAuditLog = osModel('query-audit-logs');

    QueryAuditLog.list = function(filterOpts) {
      filterOpts            = filterOpts || {};
      filterOpts.startAt    = filterOpts.startAt || 0;
      filterOpts.maxResults = filterOpts.maxResults || 25;
      return QueryAuditLog.query(filterOpts);
    }

    QueryAuditLog.exportLogs = function(criteria) {
      return $http.post(QueryAuditLog.url() + 'export', criteria).then(
        function(resp) {
          return resp.data;
        }
      );
    }

    return QueryAuditLog;
  });
