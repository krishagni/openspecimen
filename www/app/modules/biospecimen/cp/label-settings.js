
angular.module('os.biospecimen.cp')
  .controller('CpLabelSettingsCtrl', function($scope, $translate, $injector, cp, PvManager) {

    function init() {
      $scope.settingCtx = {
        spmnLabelPrePrintModes: PvManager.getPvs('specimen-label-pre-print-modes'),
        visitNamePrintModes: PvManager.getPvs('visit-name-print-modes')
      };
      
      if (!cp.spmnLabelPrintSettings || cp.spmnLabelPrintSettings.length == 0) {
        cp.spmnLabelPrintSettings = [
          {"lineage": "New"},
          {"lineage": "Derived"},
          {"lineage": "Aliquot"}
        ];
      }
      setViewCtx();
    };

    function setViewCtx() {
      angular.extend($scope.settingCtx, {view: 'view_setting', cp: cp});
      setUserInputLabels(cp);
      onPrePrintModeChange();
    }

    function setEditCtx() {
      angular.extend($scope.settingCtx, {view: 'edit_setting', cp: angular.copy(cp)});
      onPrePrintModeChange();
    }

    function setUserInputLabels(cp) {
      $scope.settingCtx.userInputLabels = '';
      $translate('cp.label_format.ppid').then(
        function() {
          var result = [];

          if (cp.manualPpidEnabled) {
            result.push($translate.instant('cp.label_format.ppid'));
          }

          if (cp.manualVisitNameEnabled) {
            result.push($translate.instant('cp.label_format.visit'));
          }

          if (cp.manualSpecLabelEnabled) {
            result.push($translate.instant('cp.label_format.specimen'));
          }

          $scope.settingCtx.userInputLabels = result.join(", ");
        }
      );
    }

    function onPrePrintModeChange() {
      $scope.settingCtx.prePrintDisabled = (
        $scope.settingCtx.cp.spmnLabelPrePrintMode == 'NONE' &&
        !$injector.has('Supply')
      );

      loadLabelAutoPrintModes();
      if ($scope.settingCtx.prePrintDisabled) {
        angular.forEach($scope.settingCtx.cp.spmnLabelPrintSettings,
          function(setting) {
            if (setting.printMode == 'PRE_PRINT') {
              setting.printMode = '';
            }
          }
        );
      }
    }
    
    function loadLabelAutoPrintModes() {
      $scope.settingCtx.primaryLabelAutoPrintModes = [];
      $scope.settingCtx.childLabelAutoPrintModes   = [];

      PvManager.loadPvs('specimen-label-auto-print-modes').then(
        function(pvs) {
          if ($scope.settingCtx.cp.spmnLabelPrePrintMode == 'NONE' && !$injector.has('Supply')) {
            pvs = pvs.filter(function(pv) { return pv.name != 'PRE_PRINT'; });
          }

          $scope.settingCtx.primaryLabelAutoPrintModes = pvs;
          $scope.settingCtx.childLabelAutoPrintModes = pvs.filter(function(pv) { return pv.name != 'ON_RECEIVE'; });
        }
      );
    }

    $scope.showEditForm = function() {
      setEditCtx();
    }

    $scope.onPrePrintModeChange = onPrePrintModeChange;

    $scope.save = function() {
      delete $scope.settingCtx.cp.repositoryNames;
      delete $scope.settingCtx.cp.extensionDetail;
      delete $scope.settingCtx.cp.catalogSetting;

      $scope.settingCtx.cp.$saveOrUpdate().then(
        function(savedcp) {
          angular.extend($scope.settingCtx.cp, savedcp);
          angular.extend(cp, $scope.settingCtx.cp);
          setViewCtx();
        }
      );
    };

    $scope.revertEdit = function() {
      setViewCtx();
    }

    init();
  });
