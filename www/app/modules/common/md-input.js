
angular.module('openspecimen')
  .directive('osMdInput', function($timeout, $document) {
    var value;

    function toggleLabel(labelEl, val) {
      labelEl.hide();
      if (val != undefined && val != null && val.length > 0) {
        labelEl.show();
      } else if (!!value) {
        labelEl.show();
      }
    }

    function onKeyUp(element) {
      return function() {
        var input = element.find('input');
        if (input.length <= 0) {
          return;
        }

        var labelEl = element.find('label.os-md-input-label');
        input.bind('keyup', function(event) {
          toggleLabel(labelEl, input.val());
        });
      };
    };

    function handleTextArea(scope, element) {
      var threshold    = 6,
          minHeight    = element[0].offsetHeight,
          paddingLeft  = element.css('paddingLeft'),
          paddingRight = element.css('paddingRight');

      var shadow = angular.element('<div></div>').css({
        position:   'absolute',
        top:        -10000,
        left:       -10000,
        width:      element[0].offsetWidth - parseInt(paddingLeft || 0) - parseInt(paddingRight || 0),
        fontSize:   element.css('fontSize'),
        fontFamily: element.css('fontFamily'),
        lineHeight: element.css('lineHeight'),
        resize:     'none'
      });

      angular.element($document[0].body).append(shadow);

      function times (string, number) {
        for (var i = 0, r = ''; i < number; i++) {
          r += string;
        }
        return r;
      }

      function update () {
        var val = element.val().replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/&/g, '&amp;')
          .replace(/\n$/, '<br/>&nbsp;')
          .replace(/\n/g, '<br/>')
          .replace(/\s{2,}/g, function( space ) {
            return times('&nbsp;', space.length - 1) + ' ';
          });

        shadow.html(val);
        element.css('height', Math.max(shadow[0].offsetHeight + threshold , minHeight));
      }

      scope.$on('$destroy', function() {
        shadow.remove();
      });

      element.bind('keyup change' , update);
      update();
    }

    function linker(scope, element, attrs) {
      $timeout(onKeyUp(element));

      var labelEl = element.find('label.os-md-input-label');
      scope.$watch(attrs.ngModel, function(newVal) {
        value = newVal;
        toggleLabel(labelEl, newVal);
      });

      var textAreaEl = element.find('textarea');
      if (textAreaEl.length > 0) {
        handleTextArea(scope, textAreaEl);
      }
    };

    return {
      restrict: 'A',
      compile: function(tElem, tAttrs) {
        var inputDiv = angular.element('<div/>')
          .addClass('os-md-input');

        var label = angular.element('<label/>')
          .addClass('os-md-input-label')
          .append(tAttrs.placeholder);

        if (tAttrs.help && tAttrs.placeholder) {
          label.attr('help', tAttrs.help);
          label.append('<span class="fa fa-question-circle" style="display: inline-block; margin-left: 5px;"></span>');
        }

        inputDiv.append(label);
        inputDiv.append(tElem.clone().removeAttr('os-md-input').removeAttr('edit-when'));
        
        var next = tElem.next();
        if (angular.isDefined(next.hasAttribute) && next.hasAttribute('os-field-error')) {
          inputDiv.append(next.clone());
        }

        var div = undefined;
        if (tAttrs.editWhen) {
          inputDiv.attr("ng-if", tAttrs.editWhen);

          var valLabel = angular.element('<span class="os-value"/>')
            .attr("ng-if", "!(" + tAttrs.editWhen + ")")
            .attr("title", "{{" + tAttrs.ngModel + "}}")
            .append("{{" + (tAttrs.displayProp ? tAttrs.ngModel + "['" + tAttrs.displayProp + "']" : tAttrs.ngModel) + "}}");

          div = angular.element('<div class="os-md-input"></div>')
            .append(inputDiv)
            .append(valLabel);
        } else {
          div = inputDiv;
        }
 
        tElem.replaceWith(div);
        return linker;
      }
    };
  });
