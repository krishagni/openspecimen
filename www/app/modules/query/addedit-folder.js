angular.module('os.query.addeditfolder', ['os.query.models'])
  .controller('AddEditQueryFolderCtrl', function($scope, $modalInstance, folder, QueryFolder) {
    $scope.folder = folder;

    $scope.saveOrUpdateFolder = function () {
      var sharedWith = [], sharedWithGroups;
      if (!$scope.folder.sharedWithAll) {
        sharedWith = $scope.folder.sharedWith.map(function(user) { return {id: user.id} });
        sharedWithGroups = ($scope.folder.sharedWithGroups || []).map(function(group) { return {id: group.id} });
      }

      var queries = $scope.folder.queries.map(function(query) { return {id: query.id} });
        
      var folderToSave = new QueryFolder({
        id: $scope.folder.id,
        name: $scope.folder.name,
        sharedWithAll: $scope.folder.sharedWithAll,
        sharedWith: sharedWith,
        sharedWithGroups: sharedWithGroups,
        allowEditsBySharedUsers: $scope.folder.allowEditsBySharedUsers,
        queries: queries
      });
        
      folderToSave.$saveOrUpdate().then(
        function(savedFolder) {
          $modalInstance.close(savedFolder);
        }
      );
    };

    $scope.deleteFolder = function() {
      $scope.folder.$remove().then(
        function() {
          $modalInstance.close(null);
        }
      );      
    };

    $scope.cancel = function () {
      $modalInstance.dismiss('cancel');
    };

    $scope.removeQuery = function(query, idx) {
      $scope.folder.queries.splice(idx, 1);
    };
  }
);
