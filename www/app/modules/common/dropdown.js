angular.module('openspecimen')
  .directive('osSelect', function($parse) {
    function linker(scope, element, attrs) {
      if (!attrs.onChange && (attrs.required !== undefined)) {
        return;
      }

      scope.$watch(attrs.ngModel,
        function(newVal, oldVal) {
          if (newVal != oldVal) {
            if (newVal == undefined) {
              $parse(attrs.ngModel).assign(scope, null);
            }

            if (attrs.onChange) {
              scope.$eval(attrs.onChange)(newVal);
            }
          }
        }
      );
    }

    function getItemDisplayValue(item, tAttrs) {
      var result = item;
      if (!!tAttrs.displayProp) {
        result += '.' + tAttrs.displayProp;
      }
      return result;
    }  

    return {
      restrict: 'E',
      compile: function(tElem, tAttrs) {
        var multiple = angular.isDefined(tAttrs.multiple);
        var uiSelect = angular.element(multiple ? '<ui-select os-fix-dd multiple/>' : '<ui-select os-fix-dd />')
          .attr('ng-model', tAttrs.ngModel)
          .attr('ng-disabled', tAttrs.ngDisabled)
          .attr('ng-required', tAttrs.ngRequired)
          .attr('reset-search-input', true)
          .attr('append-to-body', tAttrs.appendToBody == true || tAttrs.appendToBody == 'true')
          .attr('on-open', tAttrs.onOpen)
          .attr('os-tabable', !!tAttrs.osTabable);
    
        if (tAttrs.ngInit) {
          uiSelect.attr('ng-init', tAttrs.ngInit);
        }

        if (tAttrs.onSelect) {
          uiSelect.attr('on-select', tAttrs.onSelect);
        }

        if (tAttrs.onRemove) {
          uiSelect.attr('on-remove', tAttrs.onRemove);
        }

        if (tAttrs.title) {
           uiSelect.attr('title', tAttrs.title);
        }

        var uiSelectMatch = angular.element('<ui-select-match/>')
          .attr('placeholder', tAttrs.placeholder)
          .attr('allow-clear', tAttrs.required == undefined);
        
        var searchItem = getItemDisplayValue('item', tAttrs);
        var uiSelectChoices = angular.element('<ui-select-choices/>');

        if (tAttrs.selectProp) {
          uiSelectChoices.attr('repeat', "item." + tAttrs.selectProp + " as item in " + tAttrs.list + " | filter: $select.search");
        } else {
          uiSelectChoices.attr('repeat', "item in " + tAttrs.list + " | filter: $select.search");
        }

        if (tAttrs.groupBy) {
          uiSelectChoices.attr('group-by', tAttrs.groupBy);
        }

        uiSelectChoices.append('<span ng-bind-html="' + searchItem + ' | highlight: $select.search"></span>');

        if (multiple) {
          uiSelectMatch.append('{{' + getItemDisplayValue('$item', tAttrs) + '}}');
        } else {
          uiSelectMatch.append('{{' + getItemDisplayValue('$select.selected', tAttrs) + '}}');
        }

        if (angular.isDefined(tAttrs.refresh)) {
          uiSelectChoices.attr({
            'refresh': tAttrs.refresh + '($select.search, ' + tAttrs.refreshArgs + ')',
            'refresh-delay': tAttrs.refreshDelay || 750
          });
        }
            
        uiSelect.append(uiSelectMatch).append(uiSelectChoices);

        var selectContainer = angular.element("<div/>")
          .addClass("os-select-container")
          .append(uiSelect);

        tElem.replaceWith(selectContainer);
        return linker;
      }
    };
  })
  .directive('osFixDd', function($parse, $timeout) {
    return {
      require: 'uiSelect',

      restrict: 'A',

      link: function(scope, element, attrs, $select) {
        if (attrs.onOpen) {
          var onOpenFn = $parse(attrs.onOpen);
          scope.$on('uis:activate', function() { onOpenFn(scope)(); });
        }

        if (attrs.multiple != undefined && !$select.selected) {
          $select.selected = [];
        }

        var dd = null;
        var ddCont = new MutationObserver(
          function(ddContMutations) {
            if (!ddContMutations.some(function(r) { return r.attributeName == 'class' })) {
              return;
            }

            //
            // we are interested when 'direction-up' class is added to dropdown container
            //
            if (element.attr('class').indexOf('direction-up') != -1 && !dd) {
              var ddEl = element.find('.ui-select-dropdown');
              dd = new MutationObserver(
                function(ddMutations) {
                  if (!ddMutations.some(function(r) { return r.addedNodes.length != 0 || r.removedNodes.length != 0; })) {
                    return;
                  }

                  //
                  // the dropdown should be offset to the top by its height
                  // so that the bottom appears stuck to the search input
                  //
                  ddEl[0].style.top = -1 * ddEl.outerHeight() + 'px';
                }
              );

              dd.observe(ddEl[0], {childList: true, subtree: true});
            } else if (element.attr('class').indexOf('direction-up') == -1 && dd) {
              dd.disconnect();
              dd = null;
            }
          }
        );

        ddCont.observe(element[0], {attributes: true});
        element.on('remove', function() { ddCont.disconnect(); });
      }
    }
  })

  .directive('dropdownToggle', function() {
    /* Copied from angular-ui-bootstrap to make the dropdown-toggle work for class as well */

    return {
      require: '?^dropdown',
      restrict: 'AC',
      link: function(scope, element, attrs, dropdownCtrl) {
        if (!dropdownCtrl) {
          return;
        }

        element.addClass('dropdown-toggle');

        dropdownCtrl.toggleElement = element;

        var toggleDropdown = function(event) {
          event.preventDefault();

          if (!element.hasClass('disabled') && !attrs.disabled) {
            scope.$apply(function() {
              dropdownCtrl.toggle();
            });
          }
        };

        element.bind('click', toggleDropdown);

        // WAI-ARIA
        element.attr({ 'aria-haspopup': true, 'aria-expanded': false });
        scope.$watch(dropdownCtrl.isOpen, function( isOpen ) {
          element.attr('aria-expanded', !!isOpen);
        });

        scope.$on('$destroy', function() {
          element.unbind('click', toggleDropdown);
        });
      }
    };
  })

  .directive('osDropdown', function($http, ApiUrls) {
    function loadList(ctx, scope, attrs, searchValue) {
      if (!searchValue && ctx.cache[attrs.queryParams]) {
        //
        // No user search and the default list is already cached.
        // return cached results
        //
        scope.list = ctx.cache[attrs.queryParams];
        return;
      }

      var apiEndpoint = ApiUrls.getBaseUrl() + attrs.apiUrl;
      var params = JSON.parse(attrs.queryParams);
      if (!!attrs.searchProp && !!searchValue) {
        params[attrs.searchProp] = searchValue;
      }

      $http.get(apiEndpoint, {params: params}).then(
        function(resp) {
          var list = resp.data;
          if (!!searchValue) {
            //
            // obtained list is filtered based on user search
            // no need to cache the list
            //
            scope.list = list;
            return;
          }

          var selectedVal = scope.$eval(attrs.ngModel);
          if (!attrs.searchProp || !selectedVal) {
            //
            // either no remote search is enabled or no value is pre-selected
            // cache and return the default list
            //
            scope.list = ctx.cache[attrs.queryParams] = list;
            return;
          }

          //
          // remote search is enabled and dropdown value is pre-selected
          // first check whether the pre-selected value is in default list
          //
          var found = list.some(function(ele) { return ele[attrs.selectProp] == selectedVal; });
          if (found) {
            //
            // pre-selected value found in default list
            // cache and return the default list
            //
            scope.list = ctx.cache[attrs.queryParams] = list;
            return;
          }

          //
          // pre-selected value not in default list
          // remote search for pre-selected value
          //
          params[attrs.searchProp] = selectedVal;
          $http.get(apiEndpoint, {params: params}).then(
            function(searchResp) {
              //
              // extend the default list with pre-selected value elements
              //
              list = list.concat(searchResp.data);
              scope.list = ctx.cache[attrs.queryParams] = list;
            }
          );
        }
      );
    }

    function loadListUsingFn(scope, element, attrs, searchValue) {
      scope.$eval(attrs.listFn)(scope, searchValue).then(
        function(list) {
          scope.list = list;
        }
      );
    }

    return  {
      restrict: 'E',

      scope: true,

      replace: false,

      template: function(tElem, tAttrs) {
        var attrs = {
          'name'          : tAttrs.name,
          'ng-model'      : tAttrs.ngModel,
          'select-prop'   : tAttrs.selectProp,
          'display-prop'  : tAttrs.displayProp,
          'help'          : tAttrs.help,
          'append-to-body': (tAttrs.appendToBody == true || tAttrs.appendToBody == 'true'),
          'list'          : 'list',
          'on-open'       : 'onOpen'
        };

        if (tAttrs.searchProp) {
          attrs.refresh = 'searchList($select.search)';
        }

        if (tAttrs.multiple) {
          attrs.multiple = true;
        }

        var el = angular.element('<os-select/>').attr(attrs);
        if (tAttrs.mdType == 'true') {
          el.attr('os-md-input', 'os-md-input');
        }

        if (tAttrs.required) {
          el.attr('required', 'required');
        }

        if (tAttrs.ngInit) {
          el.attr('ng-init', tAttrs.ngInit);
        }

        if (tAttrs.onSelect) {
          el.attr('on-select', tAttrs.onSelect);
        }

        return el.attr('placeholder', tAttrs.placeholder);
      },

      link: function(scope, element, attrs) {
        var ctx = {cache: {}};

        scope.list = [];
        if (attrs.options) {
          scope.list = scope.$eval(attrs.options);
        } else if (attrs.apiUrl) {
          attrs.$observe('queryParams', function() { loadList(ctx, scope, attrs); });
        } else if (attrs.listFn) {
          loadListUsingFn(scope, element, attrs);
        }

        scope.searchList = function(searchValue) {
          if (attrs.listFn) {
            loadListUsingFn(scope, element, attrs, searchValue);
          } else {
            loadList(ctx, scope, attrs, searchValue);
          }
        }

        scope.onOpen = function() {
          if (attrs.listFn) {
            loadListUsingFn(scope, element, attrs);
          }
        }
      }
    }
  });
