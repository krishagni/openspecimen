
angular.module('os.biospecimen.specimenlist')
  .controller('HomeSpecimenListCtrl', function($scope, SpecimenList) {
    var ctx;

    function init(opts) {
      ctx = $scope.ctx = {
        defList: undefined,
        lists: []
      };

      $scope.$watch('opts.searchTerm', function(newVal) { loadLists(newVal); });
    }

    function loadLists(searchTerm) {
      if (!searchTerm && ctx.defList) {
        ctx.lists = ctx.defList;
        return;
      }

      SpecimenList.query({includeStats: false, name: searchTerm, orderByStarred: true, maxResults: 25}).then(
        function(lists) {
          ctx.lists = lists;
          if (!searchTerm) {
            ctx.defList = lists;
          }
        }
      );
    }

    $scope.init = init;

    $scope.toggleStar = function(list) {
      var q = list.starred ? list.unstar() : list.star();
      q.then(
        function(result) {
          if (result.status == true) {
            list.starred = !list.starred;
          }
        }
      );
    }
  });
