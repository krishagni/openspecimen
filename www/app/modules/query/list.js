
angular.module('os.query.list', ['os.query.models'])
  .controller('QueryListCtrl', function(
    $scope, $state, $modal, currentUser, queryGlobal, folders,
    Util, SavedQuery, QueryFolder, Alerts, ListPagerOpts, VueApp) {

    var filterOpts, pagerOpts;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getQueriesCount});
      filterOpts = $scope.filterOpts = Util.filterOpts({
        searchString: '',
        max: pagerOpts.recordsPerPage + 1,
        orderByStarred: true
      });

      $scope.queryList = [];
      $scope.selectedQueries = [];

      $scope.folders = {
        allFolders: [],
        myFolders: [],
        sharedFolders: []
      };

      queryGlobal.clearQueryCtx();
      loadQueries($scope.filterOpts);
      loadAllFolders();
      Util.filter($scope, 'filterOpts', loadQueries);
    };

    function getSelectedFolder(filterOpts) {
      var folder = undefined;
      if (!!filterOpts.folderId) {
        if (filterOpts.folderId <= 0) {
          folder = {id: -1}; // all queries
        } else {
          folder = folders.find(function(folder) { return folder.id == filterOpts.folderId; });
        }
      }

      return folder;
    }

    function loadQueries(filterOpts) {
      $scope.selectedQueries = [];
      $scope.folders.selectedFolder = getSelectedFolder(filterOpts);

      if (!$scope.folders.selectedFolder || $scope.folders.selectedFolder.id <= 0) {
        var userId = null;
        if (!$scope.folders.selectedFolder) {
          userId = currentUser.id;
        }

        SavedQuery.query(angular.extend({userId: userId}, filterOpts)).then(
          function(queries) {
            pagerOpts.refreshOpts(queries);
            $scope.queryList = {queries: queries};
          }
        );
      } else {
        $scope.folders.selectedFolder.query(filterOpts).then(
          function(queries) {
            pagerOpts.refreshOpts(queries);
            $scope.queryList = {queries: queries};
          }
        );
      }
    };
 
    function loadAllFolders() {
      queryGlobal.loadFolders().then(
        function(resp) {
          $scope.folders.allFolders    = resp.allFolders;
          $scope.folders.myFolders     = resp.myFolders;
          $scope.folders.sharedFolders = resp.sharedFolders;
          $scope.folders.editableFolders = (resp.sharedFolders || []).filter(
            function(folder) {
              folder.editable = (
                folder.owner.loginName != '$system' &&
                (
                  currentUser.admin ||
                  (currentUser.instituteAdmin && currentUser.instituteId == folder.owner.instituteId)  ||
                  folder.allowEditsBySharedUsers
                )
              );
              return folder.editable;
            }
          );
        }
      );
    };

    function getQueriesCount() {
      if (!$scope.folders.selectedFolder || $scope.folders.selectedFolder.id <= 0) {
        var userId = null;
        if (!$scope.folders.selectedFolder) {
          userId = currentUser.id;
        }

        return SavedQuery.getCount(angular.extend({userId: userId}, filterOpts));
      } else {
        return $scope.folders.selectedFolder.getCount(filterOpts)
      }
    }

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

    $scope.scheduleQuery = function(query) {
      VueApp.setVueView('scheduled-jobs/-1/addedit', {queryId: query.id});
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
          loadQueries($scope.filterOpts);
          Alerts.success('queries.query_deleted', {queryTitle: query.title});
        }
      );
    };

    /**
     * Folder related actions
     */
    $scope.selectFolder = function(folder, force) {
      var opts = (force && {reload: true}) || {};
      filterOpts.folderId = folder && folder.id;
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

    $scope.pageSizeChanged = function() {
      filterOpts.max = pagerOpts.recordsPerPage + 1;
    }

    init();
  });
