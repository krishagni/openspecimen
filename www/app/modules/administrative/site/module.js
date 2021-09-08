
angular.module('os.administrative.site',
  [
    'ui.router',
    'os.administrative.site.list',
    'os.administrative.site.detail',
    'os.administrative.site.addedit'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('site-list', {
        url: '/sites?filters',
        template: '<div></div>',
        controller: function(VueApp) {
          VueApp.setVueView('sites');
        },
        parent: 'signed-in'
      })
      .state('site-addedit', {
        url: '/site-addedit/:siteId',
        template: '<div></div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('site-addedit/' + (+$stateParams.siteId > 0 ? +$stateParams.siteId : -1));
        },
        parent: 'signed-in'
      })
      .state('site-import', {
        url: '/sites-import',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'site-list', title: 'site.list'}],
              objectType: 'site',
              title: 'site.bulk_import',
              onSuccess: {state: 'site-import-jobs'}
            };
          }
        },
        parent: 'signed-in'
      })
      .state('site-import-jobs', {
        url: '/sites-import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'site-list', title: 'site.list'}],
              title: 'site.bulk_import_jobs',
              objectTypes: ['site']
            }
          }
        },
        parent: 'signed-in'
      })
      .state('site-detail', {
        url: '/sites/:siteId',
        templateUrl: 'modules/administrative/site/detail.html',
        resolve: {
          site: function($stateParams, Site) {
            return Site.getById($stateParams.siteId);
          }
        },
        controller: 'SiteDetailCtrl',
        parent: 'signed-in'
      })
      .state('site-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/site/overview.html',
        controller: function() {
        },
        parent: 'site-detail'
      });
  })
  .run(function(QuickSearchSvc) {
    var opts = {caption: 'entities.site', state: 'site-detail.overview'};
    QuickSearchSvc.register('site', opts);
  });
