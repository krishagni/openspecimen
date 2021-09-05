angular.module('os.administrative.user')
  .directive('osUserGroups', function(UserGroup) {
    function linker(scope, element, attrs, formCtrl) {
      scope.defGroups = undefined;
      scope.groups = [];

      scope.searchGroups = function(searchTerm) {
        if (scope.defGroups && (!searchTerm || scope.defGroups.length < 100)) {
          scope.groups = scope.defGroups;
          return;
        }

        UserGroup.query({query: searchTerm}).then(
          function(groups) {
            if (!scope.defGroups && !searchTerm) {
              scope.defGroups = groups;
            }

            scope.groups = groups;
          }
        );
      }
    }

    return {
      restrict: 'E',
      require: '?^osFormValidator',
      scope: true,
      replace: true,
      link : linker,
      template:
        '<os-select refresh="searchGroups($select.search)" list="groups" display-prop="name" append-to-body="true"> ' +
        '</os-select>'
    };
  });
