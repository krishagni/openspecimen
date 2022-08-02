
angular.module('os.biospecimen.specimen')
  .directive('osAddSpecimens', function($q, CollectionProtocol, SpecimenUtil, Alerts) {
    return {
      restrict: 'E',

      transclude: true,

      scope: {
        onAdd: '&',
        filterOpts: '=?',
        errorOpts: '=?',
        ctrl: '=?',
        allowVisitNames: '=?',
        scanVisitNames: '=?'
      },

      controller: function($scope) {
        if ($scope.ctrl) {
          $scope.ctrl.ctrl = this;
        }

        $scope.input = {
          useBarcode: false,
          useVisit: ($scope.allowVisitNames == true && $scope.scanVisitNames == true)
        };

        $scope.addSpecimen = function(inputs) {
          var filterOpts = $scope.filterOpts || {};
          var errorOpts = $scope.errorOpts || {};
          var labels = undefined;

          filterOpts.visitNames = filterOpts.barcode = undefined;
          if (!!$scope.input.useVisit) {
            filterOpts.visitNames = inputs;
          } else if (!!$scope.input.useBarcode) {
            filterOpts.barcode = inputs;
          } else {
            labels = inputs;
          }

          return SpecimenUtil.getSpecimens(labels, filterOpts, errorOpts).then(
            function (resp) {
              if (resp.error) {
                return false;
              }

              var specimens = resp.specimens;
              if (!specimens) {
                return false;
              }

              if (specimens.length == 0) {
                var opts = errorOpts || {};
                Alerts.error(opts.no_match || 'specimens.no_matching_spmns');
                return false;
              }

              return $q.when($scope.onAdd({specimens: specimens})).then(
                function(success) {
                  return success;
                }
              );
            }
          );
        }

        $scope.onUseVisitSelection = function() {
          if ($scope.input.useVisit) {
            $scope.input.useBarcode = false;
          }
        }

        this.getLabels = function() {
          return $scope.input.ctrl.getItems();
        }

        this.useBarcode = function() {
          return $scope.input.useBarcode || false;
        }
      },

      link: function(scope, element, attrs) {
        scope.barcodingEnabled = false;
        CollectionProtocol.getBarcodingEnabled().then(
          function(barcodingEnabled) {
            scope.barcodingEnabled = barcodingEnabled;
          }
        );
      },

      template: function(tElem, tAttrs) {
        return  '<div class="os-no-label-form">' +
                '  <div class="clearfix">' +
                '    <div class="pull-left os-text-checkbox" ng-if="barcodingEnabled && !input.useVisit">' +
                '      <div class="checkbox">' +
                '        <os-checkbox ng-model="input.useBarcode"></os-checkbox>' +
                '      </div>' +
                '      <div class="message os-ctrl-padding-top">' +
                '        <span translate="specimens.use_barcode">Use Barcode</span>' +
                '      </div>' +
                '    </div>' +
                '    <div class="pull-left os-text-checkbox" style="padding-left: 5px;" ' +
                '      ng-if="allowVisitNames && !input.useBarcode">' +
                '      <div class="checkbox">' +
                '        <os-checkbox ng-model="input.useVisit" ng-change="onUseVisitSelection()"></os-checkbox>' +
                '      </div>' +
                '      <div class="message os-ctrl-padding-top">' +
                '        <span translate="specimens.use_visit_names">Use Visit Names</span>' +
                '      </div>' +
                '    </div>' +
                '  </div>' +
                '  <os-add-items ctrl="input" on-add="addSpecimen(itemLabels)"' +
                '    placeholder="' + tAttrs.placeholder + '">' +
                '    <span ng-transclude></span>' +
                '  </os-add-items>' +
                '</div>';
      }
    }
  });
