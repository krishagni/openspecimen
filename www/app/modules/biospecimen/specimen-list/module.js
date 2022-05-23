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
        template: '<div>Unused</div>',
        controller: function(VueApp) {
          VueApp.setVueView('carts/-1', {});
        },
        parent: 'signed-in'
      })
      .state('specimen-list', {
        url: '/specimen-lists/:listId/specimens?filters',
        template: '<div>Unused</div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('carts/' + $stateParams.listId + '/specimens');
        },
        parent: 'signed-in'
      })
      .state('specimen-list-addedit', {
        url: '/specimen-lists/:listId/addedit',
        template: '<div>Unused</div>',
        controller: function($stateParams, VueApp) {
          var listId = $stateParams.listId;
          if (!listId || listId < 0) {
            listId = -1;
          }

          VueApp.setVueView('cart-addedit/' + listId, {});
        },
        parent: 'signed-in'
      });
  })

  .run(function(UrlResolver, QuickSearchSvc) {
    UrlResolver.regUrlState('specimen-list', 'specimen-list', 'listId');

    var opts = {caption: 'entities.specimen_list', state: 'specimen-list'};
    QuickSearchSvc.register('specimen_list', opts);
  });
