angular.module('openspecimen')
  .factory('HomePageSvc', function($q, $state, $translate, AuthorizationService) {
    var pageCards = [];

    var sorted = false;

    var widgets = [];

    function registerCards(cards) {
      angular.forEach(cards, registerCard);
    }

    function registerCard(card) {
      pageCards.push(card);
      sorted = false;
    }

    function loadCards() {
      if (sorted || pageCards.length == 0) {
        var q = $q.defer();
        q.resolve(pageCards);
        return q.promise;
      }

      return $translate('common.home').then(
        function() {
          angular.forEach(pageCards,
            function(card) {
              card.title       = $translate.instant(card.title);
              card.description = $translate.instant(card.description);
            }
          );

          pageCards.sort(function(c1, c2) { return c1.title.localeCompare(c2.title); });
          sorted = true;
          return pageCards;
        },

        function(msg) {
          alert('failed to load page cards: ' + msg);
        }
      );
    }

    function renderElement(opts, rootTag, idx, type) {
      var rootEl = angular.element(rootTag || '<div/>');
      if (type == 'card') {
        rootEl.addClass('os-home-item');
      }

      if (opts.ctrl) {
        rootEl.attr('ng-controller', opts.ctrl);
      }

      if (opts.showIf == 'admin') {
        rootEl.attr('show-if-admin', '');
      } else if (opts.showIf == 'institute-admin') {
        rootEl.attr('show-if-admin', 'institute');
      } else if (typeof opts.showIf == 'object') {
        var ifOpts = 'hpOpts_' + idx;
        rootEl.attr('ng-init', ifOpts + '=' + JSON.stringify(opts.showIf));
        rootEl.attr('show-if-allowed', ifOpts);
      } else if (typeof opts.showIf == 'string') {
        rootEl.attr('ng-show', opts.showIf);
      }

      angular.forEach(opts.attrs,
        function(value, key) {
          rootEl.attr(key, value);
        }
      );
        
      var link = angular.element('<a/>');
      if (opts.sref) {
        link.attr('ui-sref', opts.sref);
      } else if (opts.href) {
        link.attr('href', opts.href);
      }

      if (opts.newTab) {
        link.attr('target', '_blank')
          .attr('rel', 'noopener');
      }

      if (type == 'card') {
        var icon = angular.element('<span class="os-home-item-icon"/>')
          .append(angular.element('<span/>').addClass(opts.icon));
        link.append(icon);

        var info = angular.element('<span class="os-home-item-info"/>')
          .append(angular.element('<h3/>').append(opts.title))
          .append(angular.element('<span/>').append(opts.description));
        link.append(info);
      } else {
        link.append(angular.element('<span/>').append(opts.title));
      }

      rootEl.append(link);
      return rootEl;
    }

    function renderElements(containerEl, type) {
      return loadCards().then(
        function(cards) {
          angular.forEach(cards,
            function(card, idx) {
              var cardEl = renderElement(card, '<li/>', idx, type);
              containerEl.append(cardEl);
            }
          );

          return containerEl;
        }
      );
    }

    function registerUserWidgets(inputWidgets) {
      angular.forEach(inputWidgets, registerUserWidget);
    }

    function registerUserWidget(inputWidget) {
      widgets.push(inputWidget);
    }

    function getUserWidgets() {
      var user = AuthorizationService.currentUser();
      return widgets.filter(
        function(widget) {
          if (widget.showIf == 'institute-admin') {
            return user.admin || user.instituteAdmin;
          } else if (widget.showIf == 'admin') {
            return user.admin;
          } else if (widget.showIf) {
            return AuthorizationService.isAllowed(widget.showIf);
          } else {
            return true;
          }
        }
      );
    }

    function getMenuItems() {
      return loadCards().then(
        function(cards) {
          return cards.map(
            function(card) {
              var result = angular.extend({}, card);
              if (!result.href) {
                result.href = $state.href(result.sref, {}, {absolute: false});
              }

              return result;
            }
          );
        }
      );
    }

    return {
      registerCard: registerCard,

      registerCards: registerCards,

      renderElements: renderElements,

      registerUserWidget: registerUserWidget,

      registerUserWidgets: registerUserWidgets,

      getUserWidgets: getUserWidgets,

      getMenuItems: getMenuItems
    }
  })

  .directive('osHomePageCards', function($compile, HomePageSvc) {
    return {
      restrict: 'E',

      replace: true,

      template:
        '<ul class="os-home-items-container">' +
        '</ul>',

      link: function(scope, element, attrs) { 
        HomePageSvc.renderElements(element, 'card').then(
          function() {
            $compile(element.contents())(scope);
          }
        );
      }
    }
  })

  .directive('osAppMenuItems', function($compile, HomePageSvc) {
    return {
      restrict: 'E',

      replace: true,

      template:
        '<ul role="menu">' +
        '  <li>' +
        '    <a ui-sref="home" translate="menu.home">Home</a>' +
        '  </li>' +
        '</ul>',

      link: function(scope, element, attrs) {
        HomePageSvc.renderElements(element).then(
          function() {
            $compile(element.contents())(scope);
          }
        );
      }
    }
  })

  .directive('osUserHomePageItems', function() {
    return {
      restrict: 'E',

      replace: true,

      templateUrl: 'modules/common/user-home-page-items.html',

      scope: {
        widgets: '='
      },

      link: function(scope, element, attrs) {
        var ctx = scope.ctx = { }
        angular.forEach(scope.widgets,
          function(widget) {
            ctx[widget.name] = {name: widget.name, searchMode: false, searchTerm: ''};
          }
        );

        scope.openSearch = function(widget) {
          ctx[widget.name].searchMode = true;
        }

        scope.closeSearch = function(widget) {
          ctx[widget.name].searchMode = false;
          ctx[widget.name].searchTerm = '';
        }
      }
    }
  })

  .directive('osUserHomePageItemTmpl', function() {
    return {
      restrict: 'E',

      replace: true,

      template: '<span ng-include="widget.template"></span>',

      scope: {
        widget: '=',

        opts: '='
      },

      link: function() { }
    };
  })

  .controller('HomePageCtrl', function(
    $scope, $state, $window, $rootScope, $modal, $timeout,
    userUiState, User, HomePageSvc) {

    function init() {
      var allWidgets = HomePageSvc.getUserWidgets();
      $scope.showWidgets = true;
      $scope.showSettings = allWidgets.length > 0;
      $scope.widgets = filterWidgets(allWidgets);

      var localStore = $window.localStorage;
      if (!localStore['osReqState']) {
        return;
      }

      var reqState = JSON.parse(localStore['osReqState']);
      if (reqState.name != $rootScope.state.name) {
        delete localStore['osReqState'];
        $state.go(reqState.name, reqState.params);
      }
    }

    function filterWidgets(allWidgets) {
      if (!userUiState.widgets) {
        return angular.copy(allWidgets);
      }

      var wmap = {};
      var widgets = [];
      angular.forEach(allWidgets, function(w) { wmap[w.name] = w; });
      angular.forEach(userUiState.widgets || [],
        function(uw) {
          if (!wmap[uw.name]) {
            return;
          }

          var widget = angular.copy(wmap[uw.name]);
          angular.extend(uw, angular.extend(widget, uw));
          uw.selected = true;
          widgets.push(uw);
        }
      );

      return widgets;
    }

    $scope.openSettings = function() {
      $modal.open({
        templateUrl: 'modules/common/user-home-page-settings.html',
        controller: function($scope, $modalInstance, userWidgets, widgets) {
          var wmap = {};
          angular.forEach(widgets,
            function(w) {
              wmap[w.name] = w;
              w.selected = !userWidgets;
            }
          );

          userWidgets = userWidgets || [];
          angular.forEach(userWidgets,
            function(uw) {
              if (wmap[uw.name]) {
                angular.extend(uw, angular.extend(wmap[uw.name], uw));
              }

              delete wmap[uw.name];
            }
          );

          angular.forEach(widgets,
            function(w) {
              if (wmap[w.name]) {
                userWidgets.push(w);
              }
            }
          );

          var sctx = $scope.sctx = {
            display: true,
            widgets: userWidgets,
            widths: [1, 2, 3, 4, 5, 6],
            sortOpts: {
              handle: '.handle',
              stop: function(event, ui) {
                sctx.display = false;
                $timeout(function() { sctx.display = true; });
              }
            }
          }

          $scope.save = function() {
            var selectedWidgets = sctx.widgets.filter(
              function(widget) {
                return widget.selected;
              }
            );

            var toSave = selectedWidgets.map(function(w) { return {name: w.name, width: w.width}; });
            User.saveUiState({widgets: toSave}).then(
              function() {
                $modalInstance.close(selectedWidgets);
              }
            );
          }

          $scope.cancel = function() {
            $modalInstance.dismiss('cancel');
          }
        },
        resolve: {
          userWidgets: function() {
            return userUiState.widgets && angular.copy(userUiState.widgets);
          },

          widgets: function() {
            return angular.copy(HomePageSvc.getUserWidgets());
          }
        }
      }).result.then(
        function(selectedWidgets) {
          $scope.showWidgets = false;
          $scope.widgets = userUiState.widgets = selectedWidgets;
          $timeout(function() { $scope.showWidgets = selectedWidgets.length > 0; });
        }
      );
    }

    init();

  })

  .run(function(HomePageSvc, VueApp) {
    HomePageSvc.registerCards(
      [
        {
          showIf: {resources: ['ParticipantPhi', 'ParticipantDeid'], operations: ['Read']},
          sref: 'cp-list',
          icon: 'fa fa-calendar',
          title: 'menu.collection_protocols',
          description: 'menu.cp_desc'
        },

        {
          showIf: 'admin',
          sref: 'institute-list',
          icon: 'fa fa-institution',
          title: 'menu.institutes',
          description: 'menu.institutes_desc'
        },

        {
          showIf: {resource: 'User', operations: ['Create', 'Update']},
          sref: 'user-list({userId: - 1})',
          icon: 'fa fa-user',
          title: 'menu.users',
          description: 'menu.users_desc'
        },

        {
          showIf: {resource: 'User', operations: ['Create', 'Update']},
          sref: 'role-list',
          icon: 'fa fa-lock',
          title: 'menu.roles',
          description: 'menu.roles_desc'
        },

        {
          showIf: 'institute-admin',
          sref: 'site-list',
          icon: 'fa fa-hospital-o',
          title: 'menu.sites',
          description: 'menu.sites_desc'
        },

        {
          showIf: {resource: 'StorageContainer', operations: ['Read']},
          sref: 'container-list',
          icon: 'fa fa-dropbox',
          title: 'menu.containers',
          description: 'menu.containers_desc'
        },

        {
          showIf: {resource: 'Query', operations: ['Read']},
          href: VueApp.getVueViewUrl('queries/list'),
          icon: 'fa fa-dashboard',
          title: 'menu.queries',
          description: 'menu.queries_desc'
        },

        {
          showIf: {resources: ['Specimen', 'PrimarySpecimen'], operations: ['Read']},
          sref: 'specimen-lists',
          icon: 'fa fa-shopping-cart',
          title: 'menu.specimen_lists',
          description: 'menu.specimen_lists_desc'
        },

        {
          sref: 'form-list',
          icon: 'fa fa-copy',
          title: 'menu.forms',
          description: 'menu.forms_desc',
          attrs: {
            'ng-if': 'currentUser.admin || currentUser.instituteAdmin || currentUser.manageForms'
          }
        },

        {
          showIf: {resource: 'DistributionProtocol', operations: ['Read']},
          sref: 'dp-list',
          icon: 'fa fa-truck',
          title: 'menu.distribution_protocols',
          description: 'menu.dp_desc'
        },
          
        {
          showIf: {resource: 'Order', operations: ['Read']},
          sref: 'order-list',
          icon: 'fa fa-share',
          title: 'menu.distribution_orders',
          description: 'menu.distribution_orders_desc'
        },

        {
          showIf: {resource: 'ShippingAndTracking', operations: ['Read']},
          sref: 'shipment-list',
          icon: 'fa fa-paper-plane-o',
          title: 'menu.shipping_and_tracking',
          description: 'menu.shipping_and_tracking_desc'
        },

        {
          showIf: {resource: 'ScheduledJob', operations: ['Read']},
          sref: 'job-list',
          icon: 'fa fa-gears',
          title: 'menu.jobs',
          description: 'menu.jobs_desc'
        },

        {
          showIf: 'institute-admin',
          sref: 'consent-statement-list',
          icon: 'fa fa-file-text-o',
          title: 'menu.consent_statements',
          description: 'menu.consent_statements_desc'
        },

        {
          href: '{{userCtx.trainingUrl}}',
          icon: 'fa fa-graduation-cap',
          title: 'menu.training',
          description: 'menu.training_desc',
          newTab: true
        },

        {
          showIf: 'admin',
          sref: 'settings-list',
          icon: 'fa fa-wrench',
          title: 'menu.settings',
          description: 'menu.settings_desc'
        }
      ]
    );

    HomePageSvc.registerUserWidgets(
      [
        {
          showIf: {resources: ['ParticipantPhi', 'ParticipantDeid'], operations: ['Read']},
          name: 'collection-protocols',
          icon: 'fa fa-calendar',
          detailedList: 'cp-list',
          title: 'menu.collection_protocols',
          description: 'menu.cp_desc',
          template: 'modules/biospecimen/cp/home-list.html',
          width: 2
        },
        {
          showIf: {resources: ['Specimen', 'PrimarySpecimen'], operations: ['Read']},
          name: 'specimen-lists',
          icon: 'fa fa-shopping-cart',
          detailedList: 'specimen-lists',
          title: 'menu.specimen_lists',
          description: 'menu.specimen_lists_desc',
          template: 'modules/biospecimen/specimen-list/home-list.html',
          width: 2
        },
        {
          showIf: {resource: 'StorageContainer', operations: ['Read']},
          name: 'container-list',
          icon: 'fa fa-dropbox',
          detailedList: 'container-list',
          title: 'menu.containers',
          description: 'menu.containers_desc',
          template: 'modules/administrative/container/home-list.html',
          width: 2
        },
        {
          showIf: {resource: 'Query', operations: ['Read']},
          name: 'query-list',
          icon: 'fa fa-dashboard',
          detailedList: 'query-list',
          title: 'menu.queries',
          description: 'menu.queries_desc',
          template: 'modules/query/home-list.html',
          width: 6
        },
        {
          showIf: {resource: 'DistributionProtocol', operations: ['Read']},
          name: 'dp-list',
          icon: 'fa fa-truck',
          detailedList: 'dp-list',
          title: 'menu.distribution_protocols',
          description: 'menu.dp_desc',
          template: 'modules/administrative/dp/home-list.html',
          width: 2
        },
        {
          showIf: {resource: 'Order', operations: ['Read']},
          name: 'order-list',
          icon: 'fa fa-share',
          detailedList: 'order-list',
          title: 'menu.distribution_orders',
          description: 'menu.distribution_orders_desc',
          template: 'modules/administrative/order/home-list.html',
          width: 2
        },
        {
          showIf: {resource: 'ShippingAndTracking', operations: ['Read']},
          name: 'shipment-list',
          icon: 'fa fa-paper-plane-o',
          detailedList: 'shipment-list',
          title: 'menu.shipping_and_tracking',
          description: 'menu.shipping_and_tracking_desc',
          template: 'modules/administrative/shipment/home-list.html',
          width: 2
        }
      ]
    );
  });
