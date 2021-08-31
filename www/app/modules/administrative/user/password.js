angular.module('os.administrative.user.password', ['os.administrative.models'])
  .controller('UserPasswordCtrl', function($stateParams, VueApp) {
 
    function init() {
      var url = 'user-password-change/' + $stateParams.userId;
      VueApp.setVueView(url, {});
    }

    init();
  });
