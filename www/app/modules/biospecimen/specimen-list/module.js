angular.module('os.biospecimen.specimenlist', 
  [ 
    'os.biospecimen.specimenlist.name',
    'os.biospecimen.specimenlist.addedit',
    'os.biospecimen.specimenlist.specimensholder',
    'os.biospecimen.specimenlist.assignto'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('specimen-lists', {
        url: '/specimen-lists?filters',
        templateUrl: 'modules/biospecimen/specimen-list/lists.html',
        controller: 'SpecimenListsCtrl',
        resolve: {
          pagerOpts: function(ListPagerOpts) {
            return new ListPagerOpts({recordsPerPage: 50});
          },
          lists: function($stateParams, pagerOpts, SpecimenList, Util) {
            var defOpts = {includeStats: true, orderByStarred: true, maxResults: pagerOpts.recordsPerPage + 1};
            return SpecimenList.query(Util.filterOpts(defOpts, $stateParams.filters));
          }
        },
        parent: 'signed-in'
      })
      .state('specimen-list-root', {
        url: '/specimen-lists/:listId',
        template: '<div ui-view></div>',
        resolve: {
          list: function($stateParams, SpecimenList) {
            if ($stateParams.listId >= 0) {
              return SpecimenList.getById($stateParams.listId, {includeSpecimens: false});
            }

            return new SpecimenList();
          }
        },
        abstract: true,
        parent: 'signed-in'
      })
      .state('specimen-list', {
        url: '/specimens?filters',
        params: {
          breadcrumbs: [
            {state: 'specimen-lists', params: {}, captionKey: 'specimen_list.lists'}
          ]
        },
        templateUrl: 'modules/biospecimen/specimen-list/specimens.html',
        controller: 'SpecimenListSpecimensCtrl',
        parent: 'specimen-list-root'
      })
      .state('specimen-list-addedit', {
        url: '/addedit',
        templateUrl: 'modules/biospecimen/specimen-list/addedit.html',
        controller: 'AddEditSpecimenListCtrl',
        resolve: {
          barcodingEnabled: function(CollectionProtocol) {
            return CollectionProtocol.getBarcodingEnabled();
          }
        },
        parent: 'specimen-list-root'
      });
  })

  .run(function(UrlResolver, QuickSearchSvc) {
    UrlResolver.regUrlState('specimen-list', 'specimen-list', 'listId');

    var opts = {caption: 'entities.specimen_list', state: 'specimen-list'};
    QuickSearchSvc.register('specimen_list', opts);
  });
