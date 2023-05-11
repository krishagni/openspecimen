angular.module('openspecimen')
  .directive('osSites', function($translate, $q, Site) {
    function linker(scope, element, attrs) {
      scope.sites     = [];
      scope.reload    = true;
      scope.all_sites = $translate.instant('site.all_sites');

      if (attrs.institute) {
        scope.$watch("institute", function(newVal, oldVal) {
          if (oldVal == undefined && newVal == undefined) {
            return;
          }

          scope.reload = true;
          loadSites(scope, null, attrs);
        });
      }

      scope.searchSites = function(searchTerm) {
        if (scope.reload) {
          loadSites(scope, searchTerm, attrs);
        }
      }
    }

    function loadSites(scope, searchTerm, attrs) {
      var resp = undefined;
      if (attrs.listFn) {
        resp = scope.listFn({searchTerm: searchTerm});
      } else {
        resp = Site.list(getListOpts(scope, searchTerm, attrs));
      }


      $q.when(resp).then(
        function(siteList) {
          setSitePvs(scope, searchTerm, attrs, siteList);
        }
      );
    }

    function setSitePvs(scope, searchTerm, attrs, siteList) {
      if (!searchTerm && siteList.length < 100) {
        scope.reload = false;
      }

      if (attrs.showAllSites == true) {
        siteList.unshift(scope.all_sites);
      }

      scope.sites = siteList;
    }

    function getListOpts(scope, searchTerm, attrs) {
      var opts = {name: searchTerm};
      if (scope.institute) {
        opts.institute = scope.institute;
      }

      if (attrs.listAll) {
        opts.listAll = attrs.listAll;
      } else if (attrs.resource && attrs.operation) {
        opts.resource = attrs.resource.trim();
        opts.operation = attrs.operation.trim();
      }

      opts.excludeType = scope.excludeTypes || [];
      opts.includeType = scope.includeTypes || [];
      return opts;
    }

    return {
      restrict: 'E',

      link    : linker,

      replace : true,

      scope   : {
        ngModel      : '=',
        institute    : '=',
        excludeTypes : '=',
        includeTypes : '=',
        onSelect     : '&',
        onRemove     : '&',
        listFn       : '&'
      },

      template:
        function(tElem, tAttrs) {
          var bodyAppend = angular.isDefined(tAttrs.appendToBody) ? tAttrs.appendToBody : "true";
          var tabable = angular.isDefined(tAttrs.osTabable) ? tAttrs.osTabable : "false";
          var multiple = angular.isDefined(tAttrs.multiple) ? "multiple" : "";
          var selectOption = angular.isDefined(tAttrs.multiple) ? "$item" : "$select.selected";
          var ngRequired = angular.isDefined(tAttrs.ngRequired) ? "ng-required=\"" + tAttrs.ngRequired +"\"": "";

          var ngDisabled = "";
          if (angular.isDefined(tAttrs.ngDisabled)) {
            ngDisabled = "ng-disabled=\"$parent.$parent." + tAttrs.ngDisabled.replace(/"/g, "'") + "\"";
          }

          var mdInput = tAttrs.mdType == 'true' ? 'os-md-input' : '';
          var showLabel = tAttrs.showLabel == 'true' && tAttrs.mdType == 'true' && !!tAttrs.placeholder;

          return '' +
            '<div class="os-select-container ' + mdInput + '">' +
            '  <label class="os-md-input-label" ng-show="!!ngModel && '  + showLabel + '"> ' +
                 tAttrs.placeholder +
            '  </label> ' +
            '  <ui-select ' + multiple + ' ng-model="$parent.ngModel" reset-search-input="true"' +
            '    append-to-body="' + bodyAppend + '" os-tabable="' + tabable + '" ' + ngRequired +
            '    ' + ngDisabled +
            '    on-select="onSelect({$item: $item})" on-remove="onRemove({$item: $item})">' +
            '    <ui-select-match placeholder="' + tAttrs.placeholder + '"' +
            '      allow-clear="' + !angular.isDefined(tAttrs.required) + '">' +
            '      {{' + selectOption + '}}' +
            '    </ui-select-match>' +
            '    <ui-select-choices repeat="site in sites | filter: $select.search" ' +
            '      refresh="searchSites($select.search)" refresh-delay="750">' +
            '      <span ng-bind-html="site | highlight: $select.search"></span>' +
            '    </ui-select-choices>' +
            '  </ui-select>' +
            '</div>';
        }
    };
  });
