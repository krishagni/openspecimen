
angular.module('os.query')
  .controller('QueryAuditLogsExportCtrl', function($scope, $state, QueryAuditLog, Alerts, Util) {

    var input;

    function init() {
      input = $scope.input = {};
    }

    $scope.exportReport = function() {
      var alert = Alerts.info('queries.audit_logs.preparing_report', {}, false);
      QueryAuditLog.exportLogs(input).then(
        function(resp) {
          Alerts.remove(alert);

          if (resp.fileId) {
            Alerts.info('queries.audit_logs.downloading_report');
            Util.downloadFile(QueryAuditLog.url() + 'exported-file?fileId=' + resp.fileId);
          } else {
            Alerts.info('queries.audit_logs.report_will_be_emailed');
          }

          $state.go('query-audit-logs');
        },

        function() {
          Alerts.remove(alert);
        }
      );
    }

    init();
  });
