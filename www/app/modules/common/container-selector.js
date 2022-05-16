angular.module('openspecimen')
  .directive('osContainerSelector', function(Container, Util) {
    var extend = angular.extend;
    var defaultContainers = undefined;

    function linker(scope, element, attrs) {
      scope.filterOpts = {name: scope.name};
      scope.entityType = scope.type;
      scope.initList = false;
      scope.toggleContainerSelection = toggleContainerSelection(scope);
      scope.loadChildren = loadChildren(scope);

      scope.$watch('criteria', function(newVal, oldVal) {
        if (!newVal) {
          return;
        }

        initContainersList(scope, scope.name);
      });

      Util.filter(scope, 'filterOpts', function() { filterContainers(scope); });
    };

    function initContainersList(scope, name) {
      scope.containers = [];

      var criteria = getContainerListCriteria(scope);
      extend(criteria, !!name ? {name: name} : {topLevelContainers: true});
      Container.query(criteria).then(
        function(containers) {
          if (!name) {
            addChildPlaceholders(containers);
            defaultContainers = scope.containers = Container.flatten(containers);
          } else {
            scope.containers = containers;
          }

          addDisplayTitle(scope.containers);
          scope.initList = true;
        }
      );
    }

    function getContainerListCriteria(scope, hierarchical) {
      if (typeof hierarchical === 'undefined') {
        hierarchical = true;
      }

      var criteria = {
        onlyFreeContainers: true,
        hierarchical: hierarchical
      };

      return extend(criteria, scope.criteria);
    }

    function addChildPlaceholders(containers) {
      angular.forEach(containers, function(container) {
        container.childContainers = [{id: -1, name: 'Loading ...'}];
        container.childContainersLoaded = false;
      });
    }

    function toggleContainerSelection(scope) {
      return function(container) {
        if (scope.selectedContainer && scope.selectedContainer != container) {
          scope.selectedContainer.selected = false;
        }

        container.selected = !container.selected;
        scope.selectedContainer = container.selected ? container : {};
        scope.onToggleSelection({container: container});
      }
    }

    function loadChildren(scope) {
      return function(container) {
        if (container.childContainersLoaded) {
          return;
        }

        var idx = scope.containers.indexOf(container);
        var criteria = getContainerListCriteria(scope);
        Container.query(extend({parentContainerId: container.id}, criteria)).then(
          function(containers) {
            addChildPlaceholders(containers);
            container.childContainersLoaded = true;

            var childContainers = Container._flatten(containers, 'childContainers', container, container.depth + 1);
            var args = [idx + 1, 1].concat(childContainers)
            Array.prototype.splice.apply(scope.containers, args);
            if (containers.length == 0) {
              container.hasChildren = false;
            }

            addDisplayTitle(scope.containers);
          }
        );
      };
    }

    function filterContainers(scope) {
      if (scope.filterOpts.name.trim().length == 0) {
        if (defaultContainers) {
          scope.containers = defaultContainers;
        } else {
          initContainersList(scope);
        }

        return;
      }
 
      var criteria = getContainerListCriteria(scope, false);
      Container.query(extend({name: scope.filterOpts.name}, criteria)).then(
        function(containers) {
          scope.containers = containers;
          addDisplayTitle(scope.containers);
        }
      );
    }

    function addDisplayTitle(containers) {
      angular.forEach(containers,
        function(container) {
          container.displayTitle = container.name;
          if (container.displayName) {
            container.displayTitle = container.displayName + ' (' + container.name + ')';
          }
        }
      );
    }

    return {
      restrict: 'E',
 
      replace: true,
 
      templateUrl: 'modules/common/container-selector.html',

      scope: {
        type: '=',
        name: '=',
        criteria: '=',
        onToggleSelection: '&'
      },

      link: linker
    };
  });
