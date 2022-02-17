
angular.module('os.biospecimen.cp')
  .controller('CpImportWorkflowsCtrl', function($scope, $state, $sce, cp, CollectionProtocol, CpConfigSvc, Alerts) {
    $scope.importer  = {};
    $scope.importUrl = $sce.trustAsResourceUrl(CollectionProtocol.url() + cp.id + '/workflows-file');
    

    $scope.import = function(event) {
      event.preventDefault();
      $scope.importer.submit().then(
        function(wfDetail) {
          CpConfigSvc.setWorkflows(cp, wfDetail);
          Alerts.success('cp.workflows_imported');
          cp.draftMode = true;
          $state.go('cp-detail.overview', {cpId: cp.id});
        }
      );
    }

    $scope.cancel = function(event) {
      event.preventDefault();
      $scope.back();
    }
  });
