
angular.module('os.biospecimen.cp.detail', ['os.biospecimen.models'])
  .controller('CpDetailCtrl', function(
    $scope, $q, $translate, $modal, cp,
    CollectionProtocol, PvManager, DeleteUtil, CpSettingsReg,
    SettingUtil, Util, Alerts, osExportSvc) {

    function init() {
      //
      // SOP document names are of form: <cp id>_<actual filename>
      //
      if (!!cp.sopDocumentName) {
        cp.$$sopDocDispName = cp.sopDocumentName.substring(cp.sopDocumentName.indexOf("_") + 1);
      }

      $scope.cp = cp;
      $scope.cp.repositoryNames = cp.getRepositoryNames();
      $scope.downloadUri = CollectionProtocol.url() + cp.id + '/definition';
      $scope.workflowUri = CollectionProtocol.url() + cp.id + '/workflows-file';
      $scope.sites = PvManager.getSites();
      $scope.sysStoreSpr  = true;

      var opts = {sites: cp.repositoryNames, cp: cp.shortTitle};
      angular.extend($scope.cpResource.updateOpts, opts);
      angular.extend($scope.cpResource.deleteOpts, opts);

      CpSettingsReg.getSettings().then(
        function(settings) {
          $scope.settings = settings;
        }
      );

      SettingUtil.getSetting('biospecimen', 'store_spr').then(
        function(setting) {
          $scope.sysStoreSpr = (setting.value == 'true' || setting.value == true);
        }
      );

      $scope.cpVersioningEnabled = false;
      SettingUtil.getSetting('biospecimen', 'cp_versioning_enabled').then(
        function(setting) {
          $scope.cpVersioningEnabled = (setting.value == 'true' || setting.value == true);
        }
      );
    }

    function updateStatus(newStatus, successMsg) {
      var toUpdate = angular.copy($scope.cp);
      toUpdate.activityStatus = newStatus;
      toUpdate.$saveOrUpdate().then(
        function(savedCp) {
          $scope.cp.activityStatus = savedCp.activityStatus;
          Alerts.success(successMsg);
        }
      );
    }

    $scope.editCp = function(property, value) {
      var d = $q.defer();
      d.resolve({});
      return d.promise;
    }

    $scope.deleteCp = function() {
      DeleteUtil.delete($scope.cp, {onDeleteState: 'cp-list', forceDelete: true, askReason: true});
    }

    $scope.closeCp = function() {
      Util.showConfirm({
        title: 'cp.close_cp_q',
        confirmMsg: 'cp.confirm_close_cp_msg',
        ok: function() {
          updateStatus('Closed', 'cp.closed');
        }
      });
    }

    $scope.reopenCp = function() {
      updateStatus('Active', 'cp.reopened');
    }

    $scope.exportEvents = function() {
      osExportSvc.exportRecords({objectType: 'cpe', params: {cpId: cp.id}});
    }

    $scope.exportReqs = function() {
      osExportSvc.exportRecords({objectType: 'sr', params: {cpId: cp.id}});
    }

    $scope.confirmPublish = function() {
      $modal.open({
        templateUrl: 'modules/biospecimen/cp/publish.html',
        controller: function($scope, $modalInstance) {
          var publishDetails = $scope.publishDetails = {};

          $scope.cp = cp;

          $scope.publish = function() {
            if (!publishDetails.reviewers || publishDetails.reviewers.length == 0) {
              Alerts.error('cp.publish.reviewers_req');
              return;
            }

            cp.publish(publishDetails).then(
              function() {
                Alerts.success('cp.publish.published');
                $modalInstance.close(true);
              }
            );
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      });
    }

    init();
  });
