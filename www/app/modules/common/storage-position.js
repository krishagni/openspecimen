
angular.module('openspecimen')
  .directive('osStoragePositions', function(Container) {
    return {
      restrict: 'A',

      controllerAs: '$storagePositions',

      controller: function($scope, $element, $attrs) {
        var ctrl = this;
        var locationAttr = $attrs.locationAttr || 'storageLocation';

        this.entities = [];

        this.cachedContainers = {};

        this.addEntity = function(entity) {
          ctrl.entities.push(entity);
        }

        this.clearEntities = function() {
          ctrl.entities.length = 0;
        }

        this.assignedPositions = function() {
          var assignedPositions = {};
          angular.forEach(ctrl.entities,
            function(entity) {
              if (!entity[locationAttr] || !entity[locationAttr].name) {
                return;
              }

              var positions = assignedPositions[entity[locationAttr].name];
              if (!positions) {
                assignedPositions[entity[locationAttr].name] = (positions = []);
              }

              positions.push(entity[locationAttr]);
            }
          );
          return assignedPositions;
        }

        this.getContainers = function(params) {
          var key = JSON.stringify(params);
          var q = ctrl.cachedContainers[key];
          if (!q) {
            q = ctrl.cachedContainers[key] = Container.query(params);
          }

          return q;
        }
      }
    }
  })
  .directive('osStoragePosition', function($modal, $timeout, Container) {
    function loadContainers(name, scope, ctrl) {
      var params = {
        name: name, 
        onlyFreeContainers: true
      }

      if (scope.entityType == 'specimen') {
        if (!scope.entity.type) {
          return;
        }

        angular.extend(params, {
          cpId: scope.cpId,
          specimenClass: scope.entity.specimenClass,
          specimenType: scope.entity.type,
          site: scope.site,
          storeSpecimensEnabled: true,
          maxResults: 10
        });
      } else if (scope.entityType == 'storage_container') {
        if (!scope.entity.siteName) {
          return;
        }

        angular.extend(params, {
          site: scope.entity.siteName,
          usageMode: scope.entity.usedFor || 'STORAGE'
        });
      } else if (scope.entityType == 'order_item') {
        angular.extend(params, {
          dpShortTitle: scope.dp,
          storeSpecimensEnabled: true,
          maxResults: 10
        });
      }

      var q;
      if (!!ctrl) {
        q = ctrl.getContainers(params);
      } else {
        var key = JSON.stringify(params);
        q = scope.containerListCache[key];
        if (!q) {
          q = scope.containerListCache[key] = Container.query(params);
        }
      }

      return q.then(
        function(containers) {
          scope.containers     = containers;
          containers.forEach(
            function(c) {
              c.displayTitle  = c.name;
              if (c.displayName) {
                c.displayTitle = c.displayName + ' (' + c.name + ')';
              }
            }
          );
        }
      );
    };

    function watchOccupyingEntityChanges(scope, ctrl) {
      var objType = undefined;
      if (scope.entityType == 'specimen') {
        objType = ['entity.type'];
      } else if (scope.entityType == 'storage_container') {
        objType = ['entity.siteName', 'entity.typeName', 'entity.usedFor'];
      } else {
        /* for now, nothing to observe for order items */
      }

      if (!objType) {
        return;
      }

      scope.$watchGroup(objType, function(newVal, oldVal) {
        if (!newVal || newVal == oldVal) {
          return;
        }

        loadContainers('', scope, ctrl);
      });
    }

    function invokeOnChangeCallback(scope) {
      if (scope.onChange) {
        scope.onChange({location: scope.entity[scope.locationAttr]});
      }
    }

    function addDisplayTitle(location) {
      if (!location) {
        return;
      }

      location.displayTitle = location.name;
      if (location.displayName) {
        location.displayTitle = location.displayName + ' (' + location.name + ')';
      }

    }

    function linker(scope, element, attrs, ctrl) {
      scope.containerListCache = scope.containerListCache || {};

      var entity = scope.entity || {};
      var entityType = scope.entityType = attrs.entityType || entity.getType();
      var locationAttr = scope.locationAttr = attrs.locationAttr || 'storageLocation';
      addDisplayTitle(entity[locationAttr]);

      if (!!ctrl) {
        ctrl.addEntity(entity);
      }

      scope.onContainerChange = function() {
        if (!entity[locationAttr] || !entity[locationAttr].name) {
          // case of unselect
          entity[locationAttr] = {};
          invokeOnChangeCallback(scope);
          return;
        }

        var containers = scope.containers;
        for (var i = 0; i < containers.length; ++i) {
          if (containers[i].name == entity[locationAttr].name) {
            var location = entity[locationAttr] = {
              name: containers[i].name,
              mode: containers[i].positionLabelingMode,
              displayName: containers[i].displayName
            };
            addDisplayTitle(entity[locationAttr]);
            break;
          }
        }

        invokeOnChangeCallback(scope);
      };

      scope.onPositionChange = function() {
        invokeOnChangeCallback(scope);
      }

      scope.openPositionSelector = function() {
        var modalInstance = $modal.open({
          templateUrl: 'modules/common/storage-position-selector.html',
          controller: 'StoragePositionSelectorCtrl',
          size: 'lg',
          resolve: {
            entity: function() {
              return scope.entity;
            },

            entityType: function() {
              return scope.entityType;
            },

            cpId: function() {
              return scope.cpId;
            },

            dp: function() {
              return scope.dp;
            },

            assignedPositions: function() {
              return !!ctrl ?  ctrl.assignedPositions() : {};
            },

            locationAttr: function() {
              return locationAttr;
            }
          }
        });

        modalInstance.result.then(
          function(position) {
            var location = angular.extend({}, scope.entity[locationAttr]);
            scope.entity[locationAttr] = angular.extend(location, position);
            delete location.reservationId;
            invokeOnChangeCallback(scope);
          }
        );
      };

      scope.searchContainer = function(name) {
        loadContainers(name, scope, ctrl);
      };

      scope.showStorageContainers = function () {
        scope.virtual = false;
      }

      watchOccupyingEntityChanges(scope, ctrl);
    };

    return {
      require: '?^osStoragePositions',

      restrict: 'E',
  
      replace: true,

      priority: 100,

      terminal: true,

      scope: {
        entity: '=',
        cpId: '=',
        dp: '=',
        virtual: "=",
        containerListCache: '=?',
        site: '=?',
        onChange: '&'
      },

      compile: function(tElem, tAttrs) {
        var select = tElem.find('os-select');
        var input = tElem.find('input');

        if (tAttrs.hasOwnProperty('osMdInput')) {
          var button = tElem.find('button');

          select.attr('os-md-input', '');
          input.attr('os-md-input', '');

          if (tAttrs.hasOwnProperty('editWhen')) {
            select.attr('edit-when', tAttrs.editWhen);
            input.attr('edit-when', tAttrs.editWhen);
            button.attr('ng-if', tAttrs.editWhen);
          }

          //
          // make button smaller in size
          // ensure button is aligned with other text fields;
          // 15 px is used to display labels for os-md-input fields
          //
          button.addClass('btn-xs');
          if (!tAttrs.hasOwnProperty('hidePlaceholder')) {
            button.css('margin-top', '15px');
          }
        }

        if (tAttrs.hasOwnProperty('hidePlaceholder')) {
          select.removeAttr('placeholder');
          input.removeAttr('placeholder');
        }

        return linker;
      },


      templateUrl: 'modules/common/storage-position.html'
    }
  })

  .directive('osDispStoragePosition', function() {
    return {
      restrict: 'E',

      replace: true,

      scope: {
        position: '='
      },

      template:
        '<span ng-switch on="!!position.name"> ' +
          '<span ng-switch-when="true"> ' +
            '<span ng-show="!position.displayName">{{position.name}}</span> ' +
            '<span ng-show="position.displayName">{{position.displayName}} ({{position.name}})</span> ' +
            '<span ng-show="position.mode == \'LINEAR\'">({{position.position}})</span> ' +
            '<span ng-show="position.mode == \'TWO_D\'">({{position.positionY}} x {{position.positionX}})</span> ' +
            '<span ng-show="position.mode == \'ALL\' && !!position.position"> ' +
              '({{position.positionY}} x {{position.positionX}}, {{position.position}}) ' +
            '</span> ' +
          '</span>' +
          '<span ng-switch-default> ' +
            '<span translate="specimens.virtually_located">Virtual</span>' +
          '</span>' +
        '</span>',

      link: function() { }
    };
  });
