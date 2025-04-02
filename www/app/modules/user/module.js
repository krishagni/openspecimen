angular.module('openspecimen')
  .config(function($stateProvider) {
    $stateProvider
      .state('default-nav-buttons', {
        abstract: true,
        views: {
          'nav-buttons': {
            templateUrl: 'modules/common/default-nav-btns.html',
            controller: function() { }
          },

          'app-body': {
            template: '<div ui-view></div>',
            controller: function() { }
          }
        },
        controller: function() { },
        parent: 'default'
      })
      .state('login', {
        url: '/?logout&directVisit',
        templateUrl: 'modules/user/signin.html',
        controller: function (VueApp) { VueApp.setVueView('home'); }, // 'LoginCtrl',
        parent: 'default-nav-buttons',
        data: {
          redirect: false
        },
        resolve1: {
          defCatalogId: function($injector) {
            if (!$injector.has('scCatalog')) {
              return null;
            }

            var scCatalog = $injector.get('scCatalog');
            return scCatalog.loadDefCatalogId().then(
              function(catalogId) {
                return scCatalog.defCatalogId;
              }
            );
          }
        }
      })
      .state('forgot-password', {
        url: '/forgot-password',
        templateUrl: 'modules/user/forgot-password.html',
        controller: 'ForgotPasswordCtrl',
        parent: 'default-nav-buttons'
      })
      .state('reset-password', {
        url: '/reset-password?token&loginName',
        templateUrl: 'modules/user/reset-password.html',
        controller: 'ResetPasswordCtrl',
        parent: 'default-nav-buttons',
        data: {
          redirect: false
        }
      })
      .state('sign-up', {
        url: '/sign-up',
        templateProvider: function(PluginReg, $q) {
          return $q.when(PluginReg.getTmpls("sign-up", "page-body", "modules/user/signup.html")).then(
            function(tmpls) {
              return '<div ng-include src="\'' + tmpls[0] + '\'"></div>';
            }
          );
        },
        resolve: {
          user: function(User) {
            return new User();
          },
          users: function() {
            return [];
          },
          currentUser: function() {
            return null;
          },
          editProfile: function() {
            return false;
          },
          oldView: function() {
            return true;
          }
        },
        controller: 'UserAddEditCtrl',
        parent: 'default-nav-buttons'
      })
      .state('welcome', {
        url: '/welcome',
        templateUrl: 'modules/user/welcome.html',
        resolve: {
          videoSettings : function (Setting) {            
            return Setting.getWelcomeVideoSetting();
          }
        },
        controller: 'WelcomeCtrl',
        parent: 'default-nav-buttons'
      });
  });
