angular.module('os.administrative.dp', 
  [ 
    'ui.router',
    'os.administrative.dp.list',
    'os.administrative.dp.detail',
    'os.administrative.dp.addedit',
    'os.administrative.dp.history',
    'os.administrative.dp.requirement',
    'os.administrative.dp.consents'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('dp-root', {
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope, AuthorizationService) {
          // Distribution Protocol Authorization Options
          $scope.dpResource = {
            createOpts: {resource: 'DistributionProtocol', operations: ['Create']},
            deleteOpts: {resource: 'DistributionProtocol', operations: ['Delete']},
            importOpts: {resource: 'DistributionProtocol', operations: ['Export Import']}
          }

          $scope.dpDeleteAllowed = AuthorizationService.isAllowed($scope.dpResource.deleteOpts);
        },
        parent: 'signed-in'
      })
      .state('dp-list', {
        url: '/dps?filters',
        templateUrl: 'modules/administrative/dp/list.html',
        controller: function($state, VueApp) {
          VueApp.setVueView('dps/-1', $state.params);
        },
        parent: 'dp-root'
      })
      .state('dp-addedit', {
        url: '/dp-addedit/:dpId',
        templateUrl: 'modules/administrative/dp/addedit.html',
        resolve: {
          distributionProtocol: function($stateParams , DistributionProtocol) {
            if ($stateParams.dpId) {
              return DistributionProtocol.getById($stateParams.dpId);
            }
            return new DistributionProtocol();
          },
          extensionCtxt: function(DistributionProtocol) {
            return DistributionProtocol.getExtensionCtxt();
          }
        },
        controller: 'DpAddEditCtrl',
        parent: 'dp-root'
      })
      .state('dp-import', {
        url: '/dp-import',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'dp-list', title: 'dp.list'}],
              objectType: 'distributionProtocol',
              title: 'dp.bulk_import',
              onSuccess: {state: 'dp-import-jobs'}
            };
          }
        },
        parent: 'dp-root'
      })
      .state('dp-req-import', {
        url: '/dp-req-import',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'dp-list', title: 'dp.list'}],
              objectType: 'dpRequirement',
              title: 'dp.req_bulk_import',
              onSuccess: {state: 'dp-import-jobs'}
            };
          }
        },
        parent: 'dp-root'
      })
      .state('dp-import-jobs', {
        url: '/dp-import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'dp-list', title: 'dp.list'}],
              title: 'dp.bulk_import_jobs',
              objectTypes: ['distributionProtocol', 'dpRequirement']
            }
          }
        },
        parent: 'dp-root'
      })
      .state('dp-detail', {
        url: '/dps/:dpId',
        template: '<div>New UI</div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('dps/' + $stateParams.dpId + '/detail/overview');
        },
        parent: 'dp-root'
      })
      .state('dp-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/dp/overview.html',
        parent: 'dp-detail'
      })
      .state('dp-detail.consents', {
        url: '/consents',
        templateUrl: 'modules/administrative/dp/consents.html',
        parent: 'dp-detail',
        resolve: {
          hasEc: function($injector) {
            return $injector.has('ecValidation');
          },
          consentTiers: function(hasEc, distributionProtocol) {
            return hasEc ? [] : distributionProtocol.getConsentTiers();
          }
        },
        controller: 'DpConsentsCtrl'
      })
      .state('dp-detail.history', {
        url: '/history',
        templateUrl: 'modules/administrative/dp/history.html',
        controller: 'DpHistoryCtrl',
        parent: 'dp-detail'
      })
      .state('dp-detail.reserved-specimens', {
        url: '/reserved-specimens',
        templateUrl: 'modules/administrative/dp/reserved-specimens.html',
        controller: 'DpReservedSpecimensCtrl',
        parent: 'dp-detail'
      });
  })

  .run(function(UrlResolver, QuickSearchSvc) {
    UrlResolver.regUrlState('dp-overview', 'dp-detail.overview', 'dpId');

    var opts = {caption: 'entities.distribution_protocol', state: 'dp-detail.overview'};
    QuickSearchSvc.register('distribution_protocol', opts);
  });
