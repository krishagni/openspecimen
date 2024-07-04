angular.module('os.administrative.setting', 
  [ 
    'os.administrative.setting.list',
    'os.administrative.setting.util'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('settings-list', {
        url: '/settings/settings-list?moduleName',
        template: '<div></div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('settings', $stateParams);
        },
        parent: 'signed-in'
      });
  });
