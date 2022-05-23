
angular.module('os.biospecimen.specimenlist.assignto',[])
  .directive('osAssignToSpmnList', function(SpecimenList, $rootScope, Util, VueApp) {

    function filterLists(scope) {
      return function(filterOpts) { loadSpecimenLists(scope, filterOpts); };
    }

    function loadSpecimenLists(scope, filterOpts) {
      SpecimenList.query(filterOpts).then(
        function(lists) {
          scope.specimenLists = lists;
        }
      );
    }

    return {
      restrict: 'E',

      replace: true,

      scope: {
        onAddToList: '&'
      },

      templateUrl: 'modules/biospecimen/specimen-list/assign-to-spmn-list.html',

      link: function(scope, element, attrs) {
        scope.cartsListUrl = VueApp.getVueViewUrl('carts/-1', {});
        scope.leftAlign = (attrs.menuAlign == 'left');
        scope.filterOpts = {};
        loadSpecimenLists(scope);

        scope.addToList = function(list) {
          scope.onAddToList({list: list});
        }

        scope.addToDefaultList = function() {
          var defList = new SpecimenList({
            id: 0, 
            name: '$$$$user_' + $rootScope.currentUser.id, 
            owner: $rootScope.currentUser
          });

          scope.onAddToList({list: defList});
        }

        Util.filter(scope, 'filterOpts', filterLists(scope));
      }
    }
  });
