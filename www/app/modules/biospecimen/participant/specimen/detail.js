angular.module('os.biospecimen.specimen.detail', [])
  .controller('SpecimenDetailCtrl', function(
    $scope, $state, $modal, $stateParams, currentUser, cp, listView, cpr, visit, specimen,
    ParticipantSpecimensViewState, Specimen, SpecimenUtil, SpecimenLabelPrinter, SpecimensHolder, DeleteUtil, Alerts) {

    function init() {
      $scope.cpr = cpr;
      $scope.visit = visit;
      $scope.specimen = specimen;
      $scope.treeSpecimens = getTreeSpecimens(specimen);
      $scope.statusCss = SpecimenUtil.getStatusCss(specimen);
    }

    function getTreeSpecimens(specimen) {
      var result = [];
      return result.concat(specimen.children);
    }

    function specimenUpdatedInline() {
      ParticipantSpecimensViewState.specimensUpdated($scope, {inline: true});
    }

    function specimenUpdated() {
      ParticipantSpecimensViewState.specimensUpdated($scope);
    }

    $scope.reload = function() {
      var promise = null;
      if ($stateParams.specimenId) {
        promise = Specimen.getById($stateParams.specimenId);
      } else if ($stateParams.srId) {
        promise = Specimen.getAnticipatedSpecimen($stateParams.srId);
      }

      return promise.then(
        function(specimen) {
          $scope.treeSpecimens = getTreeSpecimens(specimen);
        }
      );
    }

    $scope.reopen = function() {
      specimen.reopen().then(
        function() {
          $scope.statusCss = SpecimenUtil.getStatusCss(specimen);
          specimenUpdatedInline();
        }
      );
    }

    $scope.deleteSpecimen = function() {
      var specimen = new Specimen({
        id: $scope.specimen.id,
        label: $scope.specimen.label
      });
      var params = {cpId: cpr.cpId, cprId: cpr.id, visitId: visit.id};
      var parentId = $scope.specimen.parentId;

      DeleteUtil.delete(
        specimen,
        {
          onDeletion: function() {
            specimenUpdated();

            if (!parentId) {
              $state.go(!cp.specimenCentric ? 'visit-detail.overview' : listView, params);
            } else {
              $state.go('specimen-detail.overview', angular.extend({specimenId: parentId}, params));
            }
          },
          forceDelete: true,
          askReason: true
        }
      );
    }

    $scope.closeSpecimen = function() {
      $modal.open({
        templateUrl: 'modules/biospecimen/participant/specimen/close.html',
        controller: 'SpecimenCloseCtrl',
        resolve: {
          specimens: function() {
            return [specimen];
          }
        }
      }).result.then(
        function(result) {
          specimen.storageLocation = undefined;
          specimen.activityStatus = result[0].activityStatus;
          specimen.availabilityStatus = result[0].availabilityStatus;
          $scope.statusCss = SpecimenUtil.getStatusCss(specimen);
          specimenUpdatedInline();
        }
      );
    }

    $scope.addSpecimensToSpecimenList = function(list) {
      var selectedSpecimens = [{id: $scope.specimen.id}];

      if (!!list) {
        list.addSpecimens(selectedSpecimens).then(function(specimens) {
          var listType = list.getListType(currentUser);
          Alerts.success('specimen_list.specimens_added_to_' + listType, list);
        })
      } else {
        SpecimensHolder.setSpecimens(selectedSpecimens);
        $state.go('specimen-list-addedit', {listId: ''});
      }
    }

    init();
  });
