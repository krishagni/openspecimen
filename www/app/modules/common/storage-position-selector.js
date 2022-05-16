
angular.module('openspecimen')
  .controller('StoragePositionSelectorCtrl',
    function($scope, $modalInstance, $timeout, $q,
      entity, entityType, cpId, dp,
      assignedPositions, locationAttr, Container) {

      function init() {
        $scope.listOpts = { 
          type: entityType,
          name: !!entity[locationAttr] ? entity[locationAttr].name : ''
        };

        if (entityType == 'specimen') {
          $scope.listOpts.criteria = {
            storeSpecimensEnabled: true,
            specimenClass: entity.specimenClass,
            specimenType: entity.type,
            cpId: cpId
          }
        } else if (entityType == 'storage_container') {
          $scope.listOpts.criteria = {
            site: entity.siteName,
            usageMode: entity.usedFor || 'STORAGE'
          }
        } else if (entityType == 'order_item') {
          $scope.listOpts.criteria = {
            storeSpecimensEnabled: true,
            dpShortTitle: dp
          }
        }

        $scope.selectedContainer = {}; // step 1
        $scope.selectedPos = {};       // step 2
        $scope.showGrid = false;       // when to draw and show occupancy grid
        $scope.entity = entity;        // occupying entity for which slot is being selected
        $scope.cpId = cpId;
        $scope.assignedPositions = assignedPositions; // positions are selected in current form/session
      };

      $scope.toggleContainerSelection = function(wizard, container) {
        $scope.showGrid = false;
        if (container.selected) {
          $scope.selectedPos = {
            id: container.id,
            name: container.name,
            displayName: container.displayName,
            mode: container.positionLabelingMode,
            displayTitle: container.displayName ? (container.displayName + ' (' + container.name + ')') : container.name
          };

          $scope.selectedContainer = container;
          if (container.positionLabelingMode == 'NONE') {
            $modalInstance.close($scope.selectedPos);
          } else {
            wizard.next(false);
          }
        } else {
          $scope.selectedPos = {};
          $scope.selectedContainer = {};
        }
      }

      $scope.getOccupancyMap = function() {
        if ($scope.selectedContainer.occupiedPositions) {
          // occupied positions map already loaded
          $scope.showGrid = true;
          return true;
        }

        return Container.getById($scope.selectedContainer.id).then(
          function(container) {
            angular.extend($scope.selectedContainer, container);
            $scope.showGrid = true;
            return true;
          }
        );
      }

      $scope.passThrough = function() {
        return true;
      }

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };

      $scope.ok = function() {
        if (!$scope.selectedContainer || !$scope.selectedContainer.id) {
          $scope.cancel();
        } else {
          $modalInstance.close($scope.selectedPos);
        }
      };
             
      init();
    }
  );
