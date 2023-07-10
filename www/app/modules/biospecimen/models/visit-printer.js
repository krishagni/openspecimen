angular.module('os.biospecimen.models.visitnameprinter', ['os.common.models'])
  .factory('VisitNamePrinter', function(osModel, $http, Alerts) {
    var VisitNamePrinter = osModel('visit-name-printer');

    VisitNamePrinter.getTokens = function() {
      return $http.get(VisitNamePrinter.url() + 'tokens').then(
        function(resp) {
          return resp.data;
        }
      );
    }
 
    VisitNamePrinter.printNames = function(detail) {
      return $http.post(VisitNamePrinter.url(), detail).then(
        function(resp) {
          Alerts.success("visits.names_print_job_created", {jobId: resp.data.id});
          return resp.data;
        }
      );
    };

    return VisitNamePrinter;
  });
