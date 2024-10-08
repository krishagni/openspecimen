
angular.module('os.administrative.container.map', ['os.common.box', 'os.administrative.container.util'])
  .directive('osContainerMap', function($compile, BoxLayoutUtil, ContainerUtil) {
    return {
      restrict: 'EA',

      replace: true,

      scope: {
        container:    '=',
        occupancyMap: '=',
        onAddEvent:   '&',
        cellClick:    '&',
        toggleCellSelect: '&'
      },

      link: function(scope, element, attrs) {
        var allowClick = angular.isDefined(attrs.onAddEvent);

        scope.occupantClick = function(entityType, entityId) {
          scope.cellClick({entityType: entityType, entityId: entityId});
        }

        scope.onClick = function($event) {
          var target = angular.element($event.originalEvent.target);
          if (target.is("span.box") && target.parent() && target.parent().is("label.os-checkbox")) {
            return; // checkbox border
          }

          if (target.is("input[type='checkbox']")) {
            var slotEl = target.closest('.slot-element');
            if (target[0].checked) {
              slotEl.addClass('slot-assigned');
            } else {
              slotEl.removeClass('slot-assigned');
            }

            if (scope.toggleCellSelect) {
              var cell = {
                id      : target.attr('data-id'),
                posTwo  : target.attr('data-pos-y'),
                posOne  : target.attr('data-pos-x'),
                selected: target[0].checked
              }
              scope.toggleCellSelect({cell: cell});
            }

            return;
          }

          while (!target.hasClass('slot-element') && !target.is("table")) {
            target = target.parent();
          }

          if (target.attr('data-pos-x') && target.attr('data-pos-y')) {
            scope.onAddEvent()(target.attr('data-pos-x'), target.attr('data-pos-y'), target.attr('data-pos'));
          }
        };

        scope.$watch('occupancyMap', function() {
          element.children().remove();

          var showAddMarker = !scope.container.storeSpecimensEnabled;
          var opts = ContainerUtil.getOpts(scope.container, allowClick, allowClick && showAddMarker);
          opts.occupants = scope.occupancyMap;
          BoxLayoutUtil.drawLayout(element, opts);
          if (!showAddMarker) {
            element.addClass('os-color-coded-spmns');
          }

          $compile(element)(scope);
        });
      },

      template: '<div class="os-container-map-wrapper"></div>'
    };
  })

  .directive('osContainerPositionSelector', function($timeout, ContainerUtil) {
    function renderGrid(element, container, assignedPos, selectedPos) {
      return new openspecimen.ui.container.ContainerPositionSelector({
        parentEl: element,
        container: container,
        assignedPos: assignedPos,
        inputPos: selectedPos,
        containerUtil: ContainerUtil,
        onSelect: function(position) {
          $timeout(function() {
            selectedPos.positionX = position.posX;
            selectedPos.positionY = position.posY;
            selectedPos.position  = position.pos;
          });
        }
      }).render();
    };

    return {
      restrict: 'E',

      scope: {
        container: '=',
        selectedPos: '=',
        assignedPos: '='
      },

      link: function(scope, element, attrs) {
        var grid = undefined;
        scope.$watch('container', function(newVal, oldVal) {
          if (grid && newVal == oldVal) {
            return;
          }

          if (grid) {
            grid.destroy();
            grid = undefined;
          }

          if (!scope.container) {
            return;
          }

          grid = renderGrid(element, scope.container, scope.assignedPos, scope.selectedPos);
        });
      }
    };
  });
