
angular.module('os.administrative.container')
  .controller('MoveContainerCtrl', function($scope, $modalInstance, container) {

    var oldLocation;
    function init() {
      container.storageLocation = container.storageLocation || {};
      container.transferDate = new Date();
      oldLocation = $scope.oldLocation = {
        siteName: container.siteName,
        position: angular.extend({}, container.storageLocation)
      };

      var attrs = ['childContainers', 'occupiedPositions', 'childContainersLoaded', 'hasChildren']
      angular.forEach(attrs, function(attr) { delete container[attr]; });
      $scope.container = container;
    }

    $scope.clearParentContainer = function() {
      container.storageLocation = {};
    }

    $scope.cancel = function() {
      $modalInstance.dismiss();
    }

    $scope.submit = function() {
      var locationChanged = (oldLocation.siteName != container.siteName) ||
        (container.storageLocation.name != oldLocation.position.name) ||
        (container.storageLocation.positionY != oldLocation.position.positionY) ||
        (container.storageLocation.positionX != oldLocation.position.positionX);
      if (!locationChanged) {
        $modalInstance.close(false);
        return;
      }

      container.$saveOrUpdate().then(
        function(savedContainer) {
          $modalInstance.close(savedContainer);
        }
      );
    }

    init();
  });
