
angular.module('os.biospecimen.cp')
  .controller('CpReportSettingsCtrl', function($scope, cp, SavedQuery, Alerts) {
    function init() {
      $scope.rptSettingsCtx = {}
      setViewCtx();
    }

    function setViewCtx() {
      var ctx = {view: 'view_settings', setting: cp.reportSettings};
      angular.extend($scope.rptSettingsCtx, ctx);
    }

    function setEditCtx() {
      var setting = angular.copy(cp.reportSettings);

      var sourceType = 'AQL';
      if (setting.dataCfg) {
        sourceType = 'CUSTOM';
        setting.dataCfg = JSON.stringify(setting.dataCfg);
      }

      if (setting.metricsCfg) {
        setting.metricsCfg = JSON.stringify(setting.metricsCfg);
      }

      var ctx = {view: 'edit_settings', sourceType: sourceType, setting: setting};
      angular.extend($scope.rptSettingsCtx, ctx);
    }

    function loadQueries(searchTerm) {
      $scope.rptSettingsCtx.queryList = SavedQuery.list({searchString: searchTerm});
    }

    function enableReport(enabled) {
      var setting = angular.copy($scope.rptSettingsCtx.setting);
      setting.enabled = enabled;

      cp.saveReportSettings(setting).then(
        function(savedSetting) {
          angular.extend(cp.reportSettings, savedSetting);
          cp.draftMode = true;
          setViewCtx();
        }
      );
    }

    $scope.showEditForm = function() {
      setEditCtx();
      loadQueries();
    }

    $scope.loadQueries = loadQueries;

    $scope.revertEdit = function() {
      setViewCtx();
    }

    $scope.save = function() {
      var setting = angular.copy($scope.rptSettingsCtx.setting);

      if ($scope.rptSettingsCtx.sourceType == 'CUSTOM') {
        setting.dataQuery = undefined;
        if (!setting.dataCfg) {
          setting.dataCfg = undefined;
        } else {
          try {
            setting.dataCfg = JSON.parse(setting.dataCfg.replace(/\n|\r/, ''));
          } catch (e) {
            Alerts.error('cp.reporting.malformed_data_cfg');
            return;
          }
        }
      } else {
        setting.dataCfg = undefined;
      }

      if (!setting.metricsCfg) {
        setting.metricsCfg = undefined;
      } else {
        try {
          setting.metricsCfg = JSON.parse(setting.metricsCfg.replace(/\n|\r/, ''));
        } catch (e) {
          Alerts.error('cp.reporting.malformed_metrics');
          return;
        }
      }

      cp.saveReportSettings(setting).then(
        function(savedSetting) {
          angular.extend(cp.reportSettings, savedSetting);
          cp.draftMode = true;
          setViewCtx();
        }
      );
    }

    $scope.enableReport = function() {
      enableReport(true);
    }

    $scope.disableReport = function() {
      enableReport(false);
    }

    $scope.delete = function() {
      cp.deleteReportSettings().then(
        function() {
          cp.reportSettings = {};
          cp.draftMode = true;
          setViewCtx();
        }
      );
    }

    init();
  });
