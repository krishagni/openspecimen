angular.module('os.administrative.shipment', 
  [ 
    'ui.router',
    'os.administrative.shipment.list',
    'os.administrative.shipment.addedit',
    'os.administrative.shipment.detail',
    'os.administrative.shipment.receive'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('shipment-root', {
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope) {
          // Shipment Authorization Options
          $scope.shipmentResource = {
            createOpts: {resource: 'ShippingAndTracking', operations: ['Create']},
            updateOpts: {resource: 'ShippingAndTracking', operations: ['Update']},
            deleteOpts: {resource: 'ShippingAndTracking', operations: ['Delete']},
            importOpts: {resource: 'ShippingAndTracking', operations: ['Export Import']}
          }
        },
        parent: 'signed-in'
      })
      .state('shipment-list', {
        url: '/shipments?filters',
        template: '<div></div>',
        controller: function($state, VueApp) {
          VueApp.setVueView('shipments/-1', $state.params);
        },
        parent: 'shipment-root'
      })
      .state('shipment-addedit', {
        url: '/shipment-addedit/:shipmentId?type',
        template: '<div></div>',
        controller: function($state, $window, VueApp) {
          VueApp.setVueView($state.href($state.current.name, $state.params).substring(2));
        },
        parent: 'shipment-root'
      })
      .state('shipment-import', {
        url: '/shipment-import?type',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function($stateParams) {
            return {
              breadcrumbs: [{state: 'shipment-list', title: 'shipments.list'}],
              objectType: ($stateParams.type || 'shipment'),
              csvType: 'MULTIPLE_ROWS_PER_OBJ',
              title: 'shipments.bulk_import',
              onSuccess: {state: 'shipment-import-jobs'}
            };
          }
        },
        parent: 'signed-in'
      })
      .state('shipment-import-jobs', {
        url: '/shipment-import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'shipment-list', title: 'shipments.list'}],
              title: 'shipments.bulk_import_jobs',
              objectTypes: ['shipment', 'containerShipment']
            }
          }
        },
        parent: 'signed-in'
      })
      .state('shipment-detail', {
        url: '/shipments/:shipmentId',
        template: '<div></div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('shipments/' + $stateParams.shipmentId + '/detail/overview');
        },
        parent: 'shipment-root'
      })
      .state('shipment-detail.overview', {
        url: '/overview',
        template: '<div></div>',
        controller: function() { },
        parent: 'shipment-detail'
      })
      .state('shipment-detail.specimens', {
        url: '/specimens',
        templateUrl: 'modules/administrative/shipment/specimens.html',
        template: '<div></div>',
        controller: function() { },
        parent: 'shipment-detail',
      })
      .state('shipment-detail.containers', {
        url: '/containers',
        template: '<div></div>',
        controller: function() { },
        parent: 'shipment-detail',
      })
      .state('shipment-receive', {
        url: '/shipments/:shipmentId/receive',
        template: '<div></div>',
        controller: function($state, VueApp) {
          VueApp.setVueView($state.href($state.current.name, $state.params).substring(2));
        },
        parent: 'shipment-root'
      })
  })
  .run(function(QuickSearchSvc) {
    var opts = {caption: 'entities.shipment', state: 'shipment-detail.overview'};
    QuickSearchSvc.register('shipment', opts);
  });
