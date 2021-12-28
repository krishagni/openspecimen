angular.module('os.biospecimen.common.specimenprops', [])
  .factory('SpecimenPropsSvc', function($http, $q, ApiUrls) {
    var callQ = undefined;
    var specimenPropsMap = undefined;

    function initCall() {
      callQ = $http.get(ApiUrls.getBaseUrl() + '/specimen-type-props');
      return callQ;
    }

    function initPropsMap() {
      return callQ.then(
        function(result) {
          var tempMap = {};
          angular.forEach(result.data,
            function(prop) {
              var clsProps = tempMap[prop.specimenClass];
              if (!clsProps) {
                tempMap[prop.specimenClass] = clsProps = {};
              }

              clsProps[prop.specimenType] = prop;
            }
          );

          return (specimenPropsMap = tempMap);
        }
      )
    }

    function getPropsFromMap(cls, type) {
      var clsProps = specimenPropsMap[cls];
      if (!clsProps) {
        return undefined;
      }

      var typeProps = undefined;
      if (!!clsProps[type]) {
        typeProps = clsProps[type];
      }

      return typeProps;
    }

    function getProps(cls, type) {
      var d = $q.defer();

      if (specimenPropsMap) {
        var props = getPropsFromMap(cls, type);
        d.resolve(props);
        return d.promise;
      }

      if (!callQ) {
        initCall();
      }

      initPropsMap().then(
        function() {
          var props = getPropsFromMap(cls, type);
          d.resolve(props);
        }
      );

      return d.promise;
    }

    return {
      getProps: getProps,

      getPropsFromMap: getPropsFromMap
    }
  })
  .directive('osSpecimenTypeProp', function(SpecimenPropsSvc) {
    return {
      restrict: 'E',

      template: '<span></span>',

      replace: true,

      scope: {
        specimen: '='
      },

      link: function(scope, element, attrs) {
        if (!attrs.prop) {
          return;
        }

        scope.$watchGroup(['specimen.specimenClass', 'specimen.type'], function() {
          var spmn = scope.specimen;
          if (!spmn.specimenClass) {
            return;
          }

          SpecimenPropsSvc.getProps(spmn.specimenClass, spmn.type).then(
            function(typeProps) {
              var props = typeProps.props || {};
              element.html(props[attrs.prop]);
            }
          );
        });
      }
    }
  })
  .directive('osSpecimenUnit', function(SpecimenPropsSvc) {
    return {
      restrict: 'E',

      template: '<span></span>',

      replace: true,

      scope: {
        specimenClass: '=',
        type: '='
      },

      link: function(scope, element, attrs) {
        scope.$watchGroup(['specimenClass', 'type'], function(newVals) {
          if (!scope.specimenClass) {
            return;
          }

          var measure = attrs.measure || 'quantity';
          SpecimenPropsSvc.getProps(scope.specimenClass, scope.type).then(
            function(specimenProps) {
              var props = specimenProps.props;
              if (measure == "quantity") {
                element.html(props.qtyHtmlDisplayCode || props.qtyUnit);
              } else {
                element.html(props.concHtmlDisplayCode || props.concUnit);
              }
            }
          );
        });
      }
    }
  })
  .directive('osSpmnMeasure', function() {
    //
    // A DOM transformation directive; therefore shares the same scope as
    // the parent element
    //
    return {
      restrict: 'E',

      replace: true,

      template:
        '<div> ' +
          '<input type="text" class="form-control" ' +
            'ng-model-options="{allowInvalid: \'true\'}" ' +
            'ng-pattern="/^[0-9]*(\\.[0-9]+)?(([e][+-]?)[0-9]+)?$/"> ' +
          '<div> ' +
            '<os-specimen-unit></os-specimen-unit>' +
          '</div> ' +
        '</div>',

      compile: function(tElem, tAttrs) {
        var inputEl = tElem.find('input');
        inputEl.attr('name',        tAttrs.name);
        inputEl.attr('ng-model',    tAttrs.quantity);
        inputEl.attr('placeholder', tAttrs.placeholder);
        inputEl.attr('ng-focus',    tAttrs.ngFocus);

        if (tAttrs.ngRequired) {
          inputEl.attr('ng-required', tAttrs.ngRequired);
        } else if (tAttrs.required != undefined) {
          inputEl.attr('required', '');
        }

        if (tAttrs.ngInit) {
          inputEl.attr('ng-init', tAttrs.ngInit);
        }

        if (tAttrs.onChange) {
          inputEl.attr('ng-change', tAttrs.onChange);
        }

        if (tAttrs.ensureRange) {
          inputEl.attr('os-ensure-range', tAttrs.ensureRange);
        }

        var unitEl = tElem.find('os-specimen-unit');
        unitEl.attr('specimen-class', tAttrs.specimen + '.specimenClass');
        unitEl.attr('type',           tAttrs.specimen + '.type');
        unitEl.attr('measure',        tAttrs.measure || 'quantity');

        if (tAttrs.mdInput != undefined) {
          tElem.addClass('os-input-addon-grp os-md-input');
          inputEl.next().addClass('os-input-addon-right os-md-input-addon');
        } else {
          tElem.addClass('input-group');
          inputEl.next().addClass('input-group-addon');
        }

        return function() { };
      }
    }
  })
  .directive("osSpmnMeasureVal", function() {
    return {
      restrict: 'E',

      scope: {
        value   : '=',
        specimen: '=',
        measure : '@'
      },

      replace: true,

      template:
        '<span class="value value-md" ng-switch="!!value || value == 0">' +
        '  <span ng-switch-when="true">' +
        '    {{value | osNumberInScientificNotation}} ' +
        '    <os-specimen-unit specimen-class="specimen.specimenClass" type="specimen.type"' +
        '      measure="{{measure || \'quantity\'}}">' +
        '    </os-specimen-unit>' +
        '  </span>' +
        '  <span ng-switch-default>{{value | osNoValue}}</span>' +
        '</span>'
    }
  })
  .filter("osSpecimenQuantity", function(SpecimenPropsSvc) {
    var propsInited = false;

    return function(input, spmn, measure) {
      if (!spmn || !spmn.specimenClass || !spmn.type) {
        return input;
      }

      if (!propsInited) {
        SpecimenPropsSvc.getProps(spmn.specimenClass, spmn.type);
        propsInited = true;
        return input; // will be called again
      }

      var props = SpecimenPropsSvc.getPropsFromMap(spmn.specimenClass, spmn.type) || {};
      props = props.props || {};
      if (measure == 'quantity') {
        return input + ' ' + (props.qtyHtmlDisplayCode || props.qtyUnit);
      } else {
        return input + ' ' + (props.concHtmlDisplayCode || props.concUnit);
      }
    }
  });
