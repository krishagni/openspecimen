
angular.module('os.biospecimen.cp.dp', [])
  .controller('CpDpSettingsCtrl', function($scope, cp, DistributionProtocol, Alerts) {
    var defDps = undefined;
    function init() {
      $scope.dpCtx = {
        cp: angular.copy(cp),
        savedDps: []
      };

      initCpDps();
    }

    function initCpDps() {
      var savedDps = $scope.dpCtx.savedDps = angular.copy(cp.distributionProtocols);
      return savedDps;
    }

    function loadDps(searchString) {
      if (defDps && (!searchString || defDps.length < 100)) {
        $scope.dpCtx.dps = defDps;
        return;
      }

      var filterOpts = {activityStatus: 'Active', query: searchString, excludeExpiredDps: true, cp: cp.shortTitle};
      DistributionProtocol.query(filterOpts).then(
        function(dps) {
          $scope.dpCtx.dps = dps;
          if (!searchString) {
            defDps = dps;

            if (dps.length == 0) {
              Alerts.error('cp.dp.no_dp');
            }
          }
        }
      );
    }

    $scope.loadDps = loadDps;

    $scope.showEditForm = function() {
      $scope.dpCtx.view = 'edit';
    }

    $scope.revertEdit = function() {
      $scope.dpCtx.cp.distributionProtocols = $scope.dpCtx.savedDps;
      $scope.dpCtx.view = 'view';
    }

    $scope.save = function() {
      return $scope.dpCtx.cp.$saveOrUpdate().then(
        function(savedCp) {
          angular.extend(cp, savedCp);
          initCpDps();
          $scope.dpCtx.view = 'view';
        }
      );
    }

    init();
  });
