
angular.module('os.query')
  .controller('HomeQueryListCtrl', function($scope, AuthorizationService, SavedQuery) {
    var ctx;

    function init(opts) {
      ctx = $scope.ctx = {
        defList: undefined,
        queries: []
      };

      $scope.$watch('opts.searchTerm', function(newVal) { loadQueries(newVal); });
    }

    function loadQueries(searchTerm) {
      if (!searchTerm && ctx.defList) {
        ctx.queries = ctx.defList;
        return;
      }

      var userId = AuthorizationService.currentUser().id;
      SavedQuery.query({searchString: searchTerm, orderByStarred: true, userId: userId, max: 25}).then(
        function(queries) {
          ctx.queries = queries;
          if (!searchTerm) {
            ctx.defList = queries;
          }
        }
      );
    }

    $scope.init = init;

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
  });
