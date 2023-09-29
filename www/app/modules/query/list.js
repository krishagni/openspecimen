
angular.module('os.query.list', ['os.query.models'])
  .controller('QueryListCtrl', function(
    $scope, $state, $modal, currentUser, queryGlobal, folder,
    Util, SavedQuery, QueryFolder, Alerts) {

    function init() {
      $scope.filterOpts = Util.filterOpts({searchString: '', orderByStarred: true});
      $scope.queryList = [];
      $scope.selectedQueries = [];
      $scope.folders = {
        selectedFolder: folder,
        allFolders: [],
        myFolders: [],
        sharedFolders: []
      };

      queryGlobal.clearQueryCtx();
      loadQueries($scope.filterOpts);
      loadAllFolders();
      Util.filter($scope, 'filterOpts', loadQueries);
    };

    function loadQueries(filterOpts) {
      $scope.selectedQueries = [];
      if (!$scope.folders.selectedFolder || $scope.folders.selectedFolder.id <= 0) {
        if (!$scope.folders.selectedFolder) {
          filterOpts.userId = currentUser.id;
        } else {
          filterOpts.userId = null;
        }

        $scope.queryList = SavedQuery.list(filterOpts);
      } else {
        $scope.queryList = $scope.folders.selectedFolder.getQueries(filterOpts);
      }
    };
 
    function loadAllFolders() {
      queryGlobal.loadFolders().then(
        function(resp) {
          $scope.folders.allFolders    = resp.allFolders;
          $scope.folders.myFolders     = resp.myFolders;
          $scope.folders.sharedFolders = resp.sharedFolders;
        }
      );
    };

    function loadFolderQueries(folder) {
      $scope.folders.selectedFolder = folder;
      $scope.filterOpts = {searchString: ''};
      loadQueries($scope.filterOpts);
    };

    $scope.viewResults = function(query) {
      $state.go('query-results', {queryId: query.id});
    }

    $scope.importQuery = function() {
      var mi = $modal.open({
        templateUrl: 'modules/query/import_query.html',
        controller: 'ImportQueryCtrl'
      });

      mi.result.then(
        function(result) {
          $scope.selectFolder(undefined, true);
          Alerts.success('queries.query_imported', {queryTitle: result.title});
        }
      );
    };

    $scope.toggleQuerySelect = function(query) {
      if (query.selected) {
        $scope.selectedQueries.push(query);
      } else {
        var idx = $scope.selectedQueries.indexOf(query);
        if (idx != -1) {
          $scope.selectedQueries.splice(idx, 1);
        }
      }
    };

    $scope.deleteQuery = function(query) {
      var mi = $modal.open({
        templateUrl: 'modules/query/confirm-delete.html',
        controller: 'DeleteQueryConfirmCtrl',
        resolve: {
          query: function() {
            return query;
          }
        }
      });

      mi.result.then(
        function(result) {
          loadFolderQueries($scope.folders.selectedFolder);     
          Alerts.success('queries.query_deleted', {queryTitle: query.title});
        }
      );
    };

    /**
     * Folder related actions
     */
    $scope.selectFolder = function(folder, force) {
      var opts = (force && {reload: true}) || {};
      $state.go('query-list', {folderId: folder && folder.id}, opts);
    };

    $scope.addSelectedQueriesToFolder = function(folder) {
      folder.addQueries($scope.selectedQueries).then(
        function(assignedQueries) {
          var params = {
            count: $scope.selectedQueries.length,
            folderName: folder.name
          };
          Alerts.success("queries.queries_assigned_to_folder", params);
        }
      );
    };

    $scope.createNewFolder = function() {
      var modalInstance = $modal.open({
        templateUrl: 'modules/query/addedit-folder.html',
        controller: 'AddEditQueryFolderCtrl',
        resolve: {
          folder: function() {
            return {queries: $scope.selectedQueries};
          }
        }
      });

      modalInstance.result.then(
        function(folder) {
          $scope.folders.allFolders.push(folder);
          $scope.folders.myFolders.push(folder);
          Alerts.success("queries.folder_created", {folderName: folder.name});
        }
      );
    };

    $scope.editFolder = function(folder) {
      var modalInstance = $modal.open({
        templateUrl: 'modules/query/addedit-folder.html',
        controller: 'AddEditQueryFolderCtrl',
        resolve: {
          folder: function() {
            return QueryFolder.getById(folder.id);
          }
        }
      });

      modalInstance.result.then(
        function(result) {
          if (result) {
            $scope.folders.selectedFolder = folder;
            angular.extend(folder, result);

            $scope.queryList = {count: result.queries.length, queries: result.queries};
            Alerts.success("queries.folder_updated", {folderName: result.name});
          } else {
            var idx = $scope.folders.myFolders.indexOf(folder);
            $scope.folders.myFolders.splice(idx, 1);

            idx = $scope.folders.allFolders.indexOf(folder);
            $scope.folders.allFolders.splice(idx, 1);

            if ($scope.folders.selectedFolder == folder) {
              $scope.selectFolder(undefined);
            } 
            Alerts.success("queries.folder_deleted", {folderName: folder.name});
          }
        }
      );
    };

    $scope.showFolderInfo = function(folder) {
      $modal.open({
        templateUrl: 'modules/query/folder-overview.html',
        controller: function($scope, $modalInstance, folderInfo) {
          $scope.folder = folderInfo;

          $scope.done = function() {
            $modalInstance.dismiss('cancel');
          }
        },
        resolve: {
          folderInfo: function() {
            return QueryFolder.getById(folder.id, {includeQueries: false});
          }
        }
      });
    }

    $scope.toggleStar = function(query) {
      var q = query.starred ? query.unstar() : query.star();
      q.then(
        function(result) {
          if (result.status == true) {
            query.starred = !query.starred;
          }
        }
      );
    }

    init();
  });
