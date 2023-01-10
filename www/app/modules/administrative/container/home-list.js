angular.module('os.administrative.container')
  .controller('HomeContainersListCtrl', function($scope, Container) {
    var ctx;

    function init(opts) {
      ctx = $scope.ctx = {
        defList: undefined,
        containers: []
      };

      $scope.$watch('opts.searchTerm', function(newVal) { loadContainers(newVal); });
    }

    function loadContainers(searchTerm) {
      if (!searchTerm && ctx.defList) {
        ctx.containers = ctx.defList;
        return;
      }

      Container.query({topLevelContainers: true, name: searchTerm, orderByStarred: true, maxResults: 25}).then(
        function(containers) {
          ctx.containers = containers;
          if (!searchTerm) {
            ctx.defList = containers;
          }
        }
      );
    }

    $scope.init = init;

    $scope.toggleStar = function(container) {
      var q = container.starred ? container.unstar() : container.star();
      q.then(
        function(result) {
          if (result.status == true) {
            container.starred = !container.starred;
          }
        }
      );
    }
  });
