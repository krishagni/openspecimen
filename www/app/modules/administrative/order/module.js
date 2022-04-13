angular.module('os.administrative.order', 
  [ 
    'ui.router',
    'os.administrative.order.list',
    'os.administrative.order.detail',
    'os.administrative.order.addedit',
    'os.administrative.order.returnspecimens'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('order-root', {
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope, printDistLabels) {
          // Storage Container Authorization Options
          $scope.orderResource = {
            createOpts: {resource: 'Order', operations: ['Create']},
            updateOpts: {resource: 'Order', operations: ['Update']},
            deleteOpts: {resource: 'Order', operations: ['Delete']},
            importOpts: {resource: 'Order', operations: ['Export Import']}
          }

          $scope.orctx = {
            printDistLabels: printDistLabels
          }
        },
        resolve: {
          printDistLabels: function(SettingUtil) {
            return SettingUtil.getSetting('administrative', 'allow_dist_label_printing').then(
              function(setting) {
                return setting.value == 'true' || setting.value == true;
              }
            );
          }
        },
        parent: 'signed-in'
      })
      .state('order-list', {
        url: '/orders?filters',
        templateUrl: 'modules/administrative/order/list.html',     
        controller: function(VueApp) {
          VueApp.setVueView('orders/-1');
        },
        parent: 'signed-in'
      })
      .state('order-addedit', {
        url: '/order-addedit/:orderId',
        template: '<div></div>',
        controller: function($state, $window, VueApp) {
          VueApp.setVueView($state.href($state.current.name, $state.params).substring(2));
        }
      })
      /*.state('order-addedit', {
        url: '/order-addedit/:orderId?requestId&specimenListId&allReservedSpmns&dpId&clearFromCart&clearCart',
        templateUrl: 'modules/administrative/order/addedit.html',
        controller: 'OrderAddEditCtrl',
        resolve: {
          specimenList: function($stateParams, SpecimenList) {
            if ($stateParams.specimenListId) {
              return SpecimenList.getById($stateParams.specimenListId);
            }

            return null;
          },

          order: function($stateParams, $q, specimenList, DistributionProtocol, DistributionOrder) {
            if (!!$stateParams.orderId) {
              return DistributionOrder.getById($stateParams.orderId);
            }

            var allReservedSpmns = undefined;
            var p = null;
            if ($stateParams.dpId && $stateParams.dpId > 0) {
              allReservedSpmns = !specimenList && ($stateParams.allReservedSpmns == 'true');
              p = DistributionProtocol.getById($stateParams.dpId);
            }

            return $q.when(p).then(
              function(dp) {
                return new DistributionOrder({
                  status: 'PENDING',
                  distributionProtocol: dp,
                  orderItems: [],
                  specimenList: specimenList,
                  allReservedSpmns: allReservedSpmns,
                  clearListId: specimenList ? specimenList.id : $stateParams.clearFromCart,
                  clearListMode: $stateParams.clearCart
                });
              }
            );
          },

          spmnRequest: function($stateParams, $injector, order) {
            var catalog;
            if ($injector.has('scCatalog')) {
              var scCatalog = $injector.get('scCatalog');
              catalog = new scCatalog({id: -1});
            }

            if (!catalog) {
              return null;
            }

            var reqId = undefined;
            if (angular.isDefined(order.id)) {
              reqId = !!order.request ? order.request.id : undefined;
            } else if (angular.isDefined($stateParams.requestId)) {
              reqId = $stateParams.requestId;
            }

            return !reqId ? null : catalog.getRequest(reqId);
          },

          requestDp: function(spmnRequest, DistributionProtocol) {
            if (spmnRequest && spmnRequest.dpId) {
              return DistributionProtocol.getById(spmnRequest.dpId);
            }

            return null;
          },

          maxSpmnsLimit: function(SettingUtil) {
            return SettingUtil.getSetting('administrative', 'max_order_spmns_ui_limit').then(
              function(setting) {
                return (setting.value && +setting.value) || 100;
              }
            )
          },

          customFields: function($injector, $q, CpConfigSvc) {
            if (!$injector.has('sdeFieldsSvc')) {
              return [];
            }

            var cpDictQ = CpConfigSvc.getDictionary(-1, []);
            var fieldsQ = CpConfigSvc.getWorkflowData(-1, 'order-addedit-specimens', []);
            return $q.all([cpDictQ, fieldsQ]).then(
              function(resps) {
                var cpDict = resps[0] || [];
                var fields = (resps[1] && resps[1].columns) || [];
                return $injector.get('sdeFieldsSvc').commonFns().overrideFields(cpDict, fields);
              }
            );
          }
        },
        parent: 'order-root'
      })*/
      .state('order-import', {
        url: '/orders-import',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function(DistributionProtocol) {
            return {
              breadcrumbs: [{state: 'order-list', title: 'orders.list'}],
              objectType: 'distributionOrder',
              csvType: 'MULTIPLE_ROWS_PER_OBJ',
              title: 'orders.bulk_import',
              onSuccess: {state: 'order-import-jobs'},
              entityLabel: 'orders.dp',
              entitiesFn: function(searchTerm) {
                var filterOpts = {activityStatus: 'Active', query: searchTerm, excludeExpiredDps: true};
                return DistributionProtocol.query(filterOpts).then(
                  function(dps) {
                    return dps.map(function(dp) { return {id: dp.id, name: dp.shortTitle}; });
                  }
                );
              },
              entities: []
            };
          }
        },
        parent: 'signed-in'
      })
      .state('order-ret-spmns-import', {
        url: '/return-specimens-import',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'order-list', title: 'orders.returned_specimens'}],
              objectType: 'returnSpecimen',
              showImportType: false,
              importType: 'CREATE',
              title: 'orders.bulk_import',
              onSuccess: {state: 'order-import-jobs'}
            };
          }
        },
        parent: 'signed-in'
      })
      .state('order-import-jobs', {
        url: '/orders-import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'order-list', title: 'orders.list'}],
              title: 'orders.bulk_import_jobs',
              objectTypes: ['distributionOrder', 'returnSpecimen']
            };
          }
        },
        parent: 'signed-in'
      })
      .state('order-detail', {
        url: '/orders/:orderId',
        template: '<div>New UI</div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('orders/' + $stateParams.orderId + '/detail/overview');
        },
        parent: 'order-root'
      })
      .state('order-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/order/overview.html',
        parent: 'order-detail'
      })
      .state('order-detail.items', {
        url: '/items',
        templateUrl: 'modules/administrative/order/items.html',
        controller: 'OrderItemsCtrl',
        parent: 'order-detail'
      })
      .state('order-return-specimens', {
        url: '/return-specimens',
        templateUrl: 'modules/administrative/order/return-specimens.html',
        controller: 'OrderReturnSpecimensCtrl',
        resolve: {
          barcodingEnabled: function(CollectionProtocol) {
            return CollectionProtocol.getBarcodingEnabled();
          }
        },
        parent: 'order-root'
      });
  }).run(function(UrlResolver, QuickSearchSvc) {
    UrlResolver.regUrlState('order-overview', 'order-detail.overview', 'orderId');

    var opts = {caption: 'entities.distribution_order', state: 'order-detail.overview'};
    QuickSearchSvc.register('distribution_order', opts);
  });;
