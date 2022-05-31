
angular.module('os.biospecimen.consentstatement', ['os.biospecimen.models'])
  .config(function($stateProvider) {
    $stateProvider
      .state('consent-statement-list', {
        url: '/consent-statements?filters',
        template: '<div>Unused</div>',
        controller: function(VueApp) {
          VueApp.setVueView('consent-statements');
        },
        parent: 'signed-in'
      })
      .state('consent-statement-addedit', {
        url: '/consent-statement-addedit/:stmtId',
        template: '<div>Unused</div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('consent-statement-addedit/' + $stateParams.stmtId, {});
        },
        parent: 'signed-in'
      });
  });
