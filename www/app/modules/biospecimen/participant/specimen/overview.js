
angular.module('os.biospecimen.specimen.overview', ['os.biospecimen.models'])
  .controller('SpecimenOverviewCtrl', function(
    $scope, $rootScope, $modal, hasDict, userRole, cpr, visit, specimen, showSpmnActivity,
    osRightDrawerSvc, Specimen, SpecimenLabelPrinter, Util, ExtensionsUtil, ParticipantSpecimensViewState) {

    function init() {
      if (hasDict) {
        ExtensionsUtil.createExtensionFieldMap(cpr.participant, hasDict);
        ExtensionsUtil.createExtensionFieldMap(visit, hasDict);
        ExtensionsUtil.createExtensionFieldMap(specimen, hasDict);
      }

      $scope.spmnCtx = {
        obj: {cpr: cpr, visit: visit, specimen: specimen, userRole: userRole},
        inObjs: ['specimen', 'calcSpecimen',],
        exObjs: ['specimen.events'],
        watcher: ['specimen.storageLocation', 'specimen.collectionEvent.user', 'specimen.receivedEvent.user'],
        showActivity: showSpmnActivity
      }

      loadActivities();

      $scope.$watch('specimen.activityStatus', function(newVal, oldVal) {
        if (newVal == oldVal) {
          return;
        }

        if (newVal == 'Closed') {
          loadActivities();
        }
      });

      if (showSpmnActivity) {
        osRightDrawerSvc.open();
      }
    }

    function loadActivities() {
      $scope.activities = [];
      specimen.getEvents(
        function(activities) {
          $scope.activities = activities.map(
            function(activity) {
              return angular.extend({global: $rootScope.global}, activity);
            }
          );
        }
      );
    };

    function printSpecimenLabels(includeChildren, children) {
      var ids = [];
      if (includeChildren) {
        ids = (children || []).map(function(spmn) { return spmn.id; });
      }

      ids.unshift(specimen.id);

      var ts = Util.formatDate(Date.now(), 'yyyyMMdd_HHmmss');
      var outputFilename = [
        cpr.cpShortTitle, cpr.ppid,
        visit.name || visit.id,
        specimen.label || specimen.id,
        ts].join('_') + '.csv';
      SpecimenLabelPrinter.printLabels({specimenIds: ids}, outputFilename);
    }

    $scope.toggleShowActivity = function() {
      $scope.spmnCtx.showActivity = !$scope.spmnCtx.showActivity;
    }

    $scope.checkout = function(specimen) {
      $modal.open({
        templateUrl: 'modules/biospecimen/participant/specimen/checkout.html',
        controller: function($scope, $modalInstance) {
          var spmn = $scope.specimen = new Specimen({
            id: specimen.id,
            storageLocation: null,
            transferTime: new Date().getTime(),
            checkout: true
          });

          $scope.checkout = function() {
            spmn.$saveOrUpdate().then(
              function(saved) {
                specimen.storageLocation = saved.storageLocation;
                specimen.checkedOut = saved.checkedOut;
                specimen.checkoutPosition = saved.checkoutPosition;
                ParticipantSpecimensViewState.specimensUpdated($scope, {inline: true});
                loadActivities();
                $modalInstance.dismiss('cancel');
              }
            );
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      });
    }

    $scope.checkin = function(specimen) {
      $modal.open({
        templateUrl: 'modules/biospecimen/participant/specimen/checkin.html',
        controller: function($scope, $modalInstance) {
          var spmn = $scope.specimen = new Specimen({
            id: specimen.id,
            storageLocation: specimen.checkoutPosition,
            transferTime: new Date().getTime()
          });

          $scope.checkin = function() {
            spmn.$saveOrUpdate().then(
              function(saved) {
                specimen.storageLocation = saved.storageLocation;
                specimen.checkedOut = saved.checkedOut;
                specimen.checkoutPosition = saved.checkoutPosition;
                ParticipantSpecimensViewState.specimensUpdated($scope, {inline: true});
                loadActivities();
                $modalInstance.dismiss('cancel');
              }
            );
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      });
    }

    $scope.printSpecimenLabels = function() {
      var children = Specimen.flatten($scope.treeSpecimens);
      if (!children || children.length <= 0) {
        printSpecimenLabels(false);
        return;
      }

      $modal.open({
        templateUrl: 'modules/biospecimen/participant/specimen/confirm-print.html',
        controller: function($scope, $modalInstance) {
          $scope.printSpecimenLabels = function(includeChildren) {
            printSpecimenLabels(includeChildren, children);
            $modalInstance.dismiss('cancel');
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      });
    }

    init();
  });
