angular.module('os.biospecimen.specimenlist.addedit', ['os.biospecimen.models'])
  .controller('AddEditSpecimenListCtrl', function(
    $scope, $state, list, barcodingEnabled,
    Alerts, SpecimenList, SpecimensHolder, DeleteUtil, SpecimenUtil, Util) {
 
    function init() { 
      $scope.list = list;
      $scope.list.isAllowedToDeleteList = isAllowedToDeleteList(); 

      $scope.input = {
        labelText: '',
        barcodingEnabled: barcodingEnabled,
        useBarcode: false
      };

      var inputSpmns = SpecimensHolder.getSpecimens();
      if (inputSpmns instanceof Array && inputSpmns.length > 0) {
        $scope.isQueryOrSpecimenPage =  true;
        $scope.input.specimenIds = inputSpmns.map(function(spmn) { return spmn.id; });
        SpecimensHolder.setSpecimens(undefined);
      }

    }

    function isAllowedToDeleteList() {
       return !!$scope.list.id &&
              !$scope.list.defaultList &&
              ($scope.list.owner.id == $scope.currentUser.id || $scope.currentUser.admin)
    }

    function updateSpecimenList(specimenList, labels) {
      if (!labels || labels.length == 0) {
        return specimenList.$saveOrUpdate();
      }

      var filterOpts = {exactMatch: true};
      if (!!$scope.input.useBarcode) {
        filterOpts.barcode = labels;
        labels = undefined;
      }

      return SpecimenUtil.getSpecimens(labels, filterOpts).then(
        function(result) {
          if (!result || !result.specimens) {
            return undefined;
          }

          specimenList.specimenIds = result.specimens.map(function(spmn) { return spmn.id; });
          return specimenList.$saveOrUpdate();
        }
      );
    }

    $scope.saveOrUpdateList = function() {
      var labels = Util.splitStr($scope.input.labelText, /,|\t|\n/);
      var promise = undefined;

      var sharedWith = $scope.list.sharedWith.map(
        function(user) { 
          return {id: user.id} 
        }
      );

      var specimenList =  new SpecimenList({
        id: $scope.list.id,
        name: $scope.list.name,
        description: $scope.list.description,
        sharedWith: sharedWith,
        sharedWithGroups: $scope.list.sharedWithGroups,
        specimenIds: $scope.input.specimenIds || []
      });

      var promise = updateSpecimenList(specimenList, labels);
      promise.then(
        function(savedList) {
          if (!savedList) {
            return;
          }

          if ($scope.isQueryOrSpecimenPage) {
            var type = savedList.getListType($scope.currentUser);
            Alerts.success('specimen_list.specimens_added_to_' + type, savedList);

            $scope.back();
          } else {
            $state.go('specimen-list', {listId: savedList.id});
          }
        }
      );
    }

    $scope.deleteList = function() {
      DeleteUtil.delete($scope.list, {
        onDeleteState: 'specimen-lists',
        deleteWithoutCheck: true
      });
    }

    init();
  }
);
