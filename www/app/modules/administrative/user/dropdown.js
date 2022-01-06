
angular.module('os.administrative.user.dropdown', ['os.administrative.models'])
  .directive('osUsers', function($filter, AuthorizationService, User) {
    function loadUsers(scope, searchTerm, ctrl, attrs) {
      var opts = angular.extend({searchString : searchTerm}, scope.filterOpts || {});
      if (attrs.queryParams) {
        angular.extend(opts, JSON.parse(attrs.queryParams));
      }

      if (attrs.hasOwnProperty('excludeContacts')) {
        opts.excludeType = 'CONTACT';
      }

      if (attrs.hasOwnProperty('includeArchived')) {
        opts.activityStatus = opts.activityStatus || 'all';
      }

      if (attrs.hasOwnProperty('includeSysUser')) {
        opts.includeSysUser = 'true';
      }

      ctrl.listLoaded = true;
      var promise;
      if (!attrs.listFn) {
        if (ctrl.formCtrl && typeof ctrl.formCtrl.getCachedValues == 'function') {
          promise = ctrl.formCtrl.getCachedValues('users', JSON.stringify(opts), function() { return User.query(opts); });
        } else {
          promise = User.query(opts);
        }
      } else {
        promise = scope.listFn(opts);
      }

      promise.then(
        function(result) {
          scope.users = result;
          loadSelectedUser(scope, result, ctrl);
        }
      );
    };

    function loadSelectedUser(scope, usersList, ctrl) {
      if (ctrl.onceLoaded) {
        return;
      }

      ctrl.onceLoaded = true;
      var selectProp = ctrl.attrs.selectProp;
      if (selectProp != 'id' || !scope.ngModel) {
        return;
      }

      var model = scope.ngModel;
      if (typeof scope.ngModel == 'object') {
        model = scope.ngModel[selectProp];
      }

      var selectedUser = usersList.find(function(user) { return user[selectProp] == model; });
      if (selectedUser) {
        scope.ngModel = selectedUser;
        return;
      }

      User.getById(model).then(
        function(user) {
          scope.ngModel = user;
          usersList.push(user);
        }
      );
    }

    return {
      restrict: 'AE',

      scope: {
        ngModel: '=ngModel',
        placeholder: '@',
        filterOpts: '=',
        defaultList: '=',
        onSelect: '&',
        listFn: '&'
      },

      require: '?^osFormValidator',

      replace: true,

      controller: function($scope, $element, $attrs) {
        var ctrl = $scope.ctrl = this;

        if ($attrs.options) {
          $scope.users = ctrl.localList = JSON.parse($attrs.options);
          ctrl.listLoaded = true;

          $scope.searchUsers = function(searchTerm) {
            if (searchTerm) {
              $scope.users = $filter('filter')(ctrl.localList, searchTerm);
            } else {
              $scope.users = ctrl.localList;
            }
          }

          return;
        }

        $scope.searchUsers = function(searchTerm) {
          if (!searchTerm && $scope.defaultList) {
            $scope.users = $scope.defaultList;
            loadSelectedUser($scope, $scope.users, ctrl);
            return;
          }

          loadUsers($scope, searchTerm, ctrl, $attrs);
        };

        $scope.$watch('filterOpts', function(newVal, oldVal) {
          if (newVal == oldVal) {
            return;
          }

          loadUsers($scope, undefined, ctrl, $attrs);
        });

        $attrs.$observe('queryParams',
          function() {
            if (!ctrl.listLoaded) {
              //
              // this is done to ensure we do not load users list twice
              // once by the attributes observer and another by the filterOpts watcher
              //
              return;
            }

            loadUsers($scope, undefined, ctrl, $attrs);
          }
        );
      },
  
      link: function(scope, element, attrs, formCtrl) {
        var ctrl = scope.ctrl;
        ctrl.formCtrl = formCtrl;
        ctrl.attrs = attrs;

        if (!scope.ngModel && attrs.hasOwnProperty('defaultCurrentUser')) {
          var user = angular.copy(AuthorizationService.currentUser() || {});
          if (attrs.selectProp) {
            user = user[attrs.selectProp];
          }

          if (attrs.hasOwnProperty('multiple')) {
            scope.ngModel = [user];
          } else {
            scope.ngModel = user;
          }
        } else if (!scope.ngModel && attrs.hasOwnProperty('multiple')) {
          scope.ngModel = [];
        }

        if (attrs.hasOwnProperty('osMdInput')) {
          element.addClass('os-md-input');
        }
      },

      template: function(tElem, tAttrs) {
        var bodyAppend = angular.isDefined(tAttrs.appendToBody) ? tAttrs.appendToBody : "true";
        var tabable = angular.isDefined(tAttrs.osTabable) ? tAttrs.osTabable : "false";

        var loopExpr = 'user in users';
        if (angular.isDefined(tAttrs.selectProp)) {
          loopExpr = 'user.' + tAttrs.selectProp + ' as ' + loopExpr;
        }

        return angular.isDefined(tAttrs.multiple) ?
              '<div class="os-select-container">' +
                '<ui-select multiple os-fix-dd ng-model="$parent.ngModel" reset-search-input="true"' +
                  (!!tAttrs.onSelect ? 'on-select="$parent.onSelect({user: $item})"' : '') +
                  ' append-to-body="' + bodyAppend + '" os-tabable="' + tabable + '">' +
                  '<ui-select-match placeholder="{{$parent.placeholder}}">' +
                    '{{$item.firstName}} {{$item.lastName}}' +
                  '</ui-select-match>' +
                  '<ui-select-choices repeat="' + loopExpr +'" refresh="searchUsers($select.search)" refresh-delay="750">' +
                    '<span ng-bind-html="user.firstName + \' \' + user.lastName | highlight: $select.search"></span>' +
                  '</ui-select-choices>' +
                '</ui-select>' +
              '</div>'

              :

              '<div class="os-select-container">' +
                '<ui-select os-fix-dd ng-model="$parent.ngModel" reset-search-input="true"' +
                  (!!tAttrs.onSelect ? 'on-select="$parent.onSelect({user: $item})"' : '') +
                  ' append-to-body="' + bodyAppend + '" os-tabable="' + tabable + '">' +
                  '<ui-select-match placeholder="{{$parent.placeholder}}" allow-clear="'+ (tAttrs.required == undefined) +'">' +
                    '{{$select.selected.firstName}} {{$select.selected.lastName}}' +
                  '</ui-select-match>' +
                  '<ui-select-choices repeat="' + loopExpr +'" refresh="searchUsers($select.search)" refresh-delay="750">' +
                    '<span ng-bind-html="user.firstName + \' \' + user.lastName | highlight: $select.search"></span>' +
                  '</ui-select-choices>' + 
                '</ui-select>' +
              '</div>';
      }
    };
  });
