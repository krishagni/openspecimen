
angular.module('openspecimen')
  .directive('osWizard', function($q) {
    return {
      restrict: 'A',

      transclude: true,

      controller: function($scope) {
        $scope.ctrl = this;
        $scope.steps = [];

        this.addStep = function(step, order) {
          if (order == undefined || order == null) {
            order = $scope.steps.length;
          } else {
            order = parseInt(order);
          }

          angular.extend(step, {order: order, selected: $scope.steps.length == 0, finished: false});
          $scope.steps.push(step);
          $scope.steps.sort(function(s1, s2) { return s1.order - s2.order; });

          angular.forEach($scope.steps, function(step) { step.selected = false; });
          $scope.selectedStep = 0;
          $scope.steps[0].selected = true;
        };

        this.previous = function() {
          this.forward = false;
          if ($scope.selectedStep == 0) {
            return false;
          }

          var onFinish = $scope.steps[$scope.selectedStep].onFinish;
          return $q.when(onFinish({forward: this.forward, wizard: this})).then(
            function(result) {
              if (!result) {
                return false;
              }

              $scope.selectedStep--;
              $scope.steps[$scope.selectedStep + 1].selected = false;
              $scope.steps[$scope.selectedStep].selected = true;
              $scope.steps[$scope.selectedStep].finished = false;
              return true;
            }
          );
        };

        this.next = function() {
          this.forward = true;

          var onFinish = $scope.steps[$scope.selectedStep].onFinish;
          return $q.when(onFinish({forward: this.forward, wizard: this})).then(
            function(result) {
              if (!result) {
                return false;
              }

              if ($scope.selectedStep == $scope.steps.length - 1) {
                return true; // should it return true or false; // earlier it was false
              }

              $scope.selectedStep++;
              $scope.steps[$scope.selectedStep - 1].finished = true;
              $scope.steps[$scope.selectedStep - 1].selected = false;
              $scope.steps[$scope.selectedStep].selected= true;
              return true;
            }
          );
        };

        this.getCurrentStep = function() {
          return $scope.selectedStep ;
        };


        var stepThrough = function(current, numSteps, fn) {
          if (current == numSteps) {
            return;
          }

          $q.when(fn()).then(
            function(result) {
              if (result) {
                stepThrough(current + 1, numSteps, fn);
              }
            }
          );
        };

        $scope.gotoStep = function(step) {
          if ($scope.selectedStep == step) {
            return;
          }

          var fn = undefined;
          var numSteps = undefined;
          if (step > $scope.selectedStep) {
            numSteps = step - $scope.selectedStep;
            fn = $scope.ctrl.next;
          } else {
            numSteps = $scope.selectedStep - step;
            fn = $scope.ctrl.previous;
          }

          stepThrough(0, numSteps, fn);
        };

        this.isFirstStep = function() {
          return $scope.selectedStep == 0;
        };

        this.isLastStep = function() {
          return $scope.selectedStep == $scope.steps.length - 1;
        };
      },

      compile: function(element, attributes, transclude) {
        return {
          pre: function (scope, element, attrs, controller) {
            scope[attrs.osWizard] = controller;
          },

          post: function (scope, element, attributes, controller) {
            scope.selectedStep = 0;
            if (scope.steps.length > 0) {
              scope.steps[0].selected = true;
            }
          }
        }
      },

      templateUrl: function(tElem, tAttrs) {
        if (tAttrs.type == 'classic') {
          return 'modules/common/classic-wizard-template.html';
        } else if (tAttrs.type == 'vertical') {
          return 'modules/common/vertical-wizard-template.html';
        }

        return 'modules/common/tube-wizard-template.html';
      }
    };
  })

  .directive('osWizardStep', function() {
    return {
      restrict: 'E',
      require : '^osWizard',
      template: '<div ng-transclude class="step-content" ng-show="selected"></div>',
      transclude: true,
      replace : true,
      scope : {
        title : '@',
        desc  : '@',
        onFinish : '&'
      },

      link: function(scope, element, attrs, wizardCtrl) {
        wizardCtrl.addStep(scope, attrs.order);
      }
    };
  });
