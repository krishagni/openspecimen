
angular.module('openspecimen')
  .factory('AuthService', function(
    $http, $rootScope, $window, $q, $sce, $timeout,
    ApiUtil, ApiUrls, SettingUtil, User) {

    var url = function() {
      return ApiUrls.getUrl('sessions');
    };

    function impersonate(user) {
      if (!user) {
        user = new User({id: -1});
      }

      if (user.id == -1 && !$http.defaults.headers.common['X-OS-API-TOKEN']) {
        return;
      }

      return user.impersonate().then(
        function(token) {
          if (token) {
            $http.defaults.headers.common['X-OS-IMPERSONATE-USER'] = token;
          } else {
            delete $http.defaults.headers.common['X-OS-IMPERSONATE-USER'];
          }

          return !!token;
        }
      );
    }

    return {
      authenticate: function(loginData) {
        return $http.post(url(), loginData).then(ApiUtil.processResp).then(
          function(resp) {
            if (resp.data && !resp.data.resetPasswordToken) {
              $http.defaults.headers.common['X-OS-API-TOKEN'] = resp.data.token;
              return impersonate(null).then(
                function() {
                  return resp;
                },

                function() {
                  return resp;
                }
              );
            }

            return resp;
          }
        );;
      },

      logout: function() {
        if (!$http.defaults.headers.common['X-OS-API-TOKEN']) {
          var qp = $q.defer();
          qp.resolve({});
          return qp.promise;
        }

        var samlEnabled = SettingUtil.getSetting('auth', 'saml_enable');
        var sloEnabled  = SettingUtil.getSetting('auth', 'single_logout');
        var that = this;

        return $q.all([samlEnabled, sloEnabled]).then(
          function(resp) {
            var q;
            if (resp[0].value == 'true' && resp[1].value == 'true') {
              $rootScope.logoutUrl = $sce.trustAsResourceUrl(ApiUrls.getServerUrl() + 'saml/logout?_nonce='+Date.now());
              $rootScope.logout = true;
              q = $q.defer();
              q.resolve({});
              q = q.promise;
            } else {
              q = $http.delete(url());
            }


            var r = $q.defer();
            $timeout(
              function() {
                q.then(
                  function() {
                    that.removeToken();
                    $rootScope.loggedIn = false;
                    delete $rootScope.reqState;
                    delete $rootScope.currentUser;
                    if ($window.localStorage['osReqState']) {
                      delete $window.localStorage['osReqState'];
                    }

                    r.resolve({});
                  }
                );
              }
            );

            return r.promise;
          }
        );
      },

      saveToken: function(token) {
        $window.localStorage['osAuthToken'] = token;
        $http.defaults.headers.common['X-OS-API-TOKEN'] = token;
        $http.defaults.withCredentials = true;
      },

      removeToken: function() {
        delete $window.localStorage['osAuthToken'];
        delete $http.defaults.headers.common['X-OS-API-TOKEN'];
        delete $http.defaults.headers.common['Authorization'];
        impersonate(null);
      },

      refreshCookie: function() {
        return $http.post(url() + "/refresh-cookie").then(ApiUtil.processResp);
      },

      impersonate: impersonate
    }
  })
  .controller('LoginCtrl', function(
    $scope, $rootScope, $state, $stateParams, $q, $http, $location, $injector, $window, $translate,
    Alerts, AuthDomain, AuthService) {

    function init() {
      $scope.errors     = [];
      $scope.loginData  = {'$$otpReq': false};
      $scope.samlDomain = '';
      $scope.showSignIn = true;
      
      var logoutQ;
      if ($location.search().logout) {
        logoutQ = $scope.logout();
      } else {
        logoutQ = $q.defer();
        logoutQ.resolve({});
        logoutQ = logoutQ.promise;
      }

      logoutQ.then(
        function() {
          if ($http.defaults.headers.common['X-OS-API-TOKEN']) {
            if ($rootScope.reqState) {
              $state.go($rootScope.reqState.name, $rootScope.reqState.params);
            } else {
              $state.go('home');
            }
            //return;
          } else if (!$stateParams.directVisit && $injector.has('scCatalog')) {
            //
            // User not logged in
            //
            $q.when(logoutQ, gotoCatalog, gotoCatalog);
          }

          if ($stateParams.directVisit == 'true') {
            $rootScope.reqState = undefined;
          }

          loadDomains();
        }
      );
    }

    function gotoCatalog() {
      if ($state.current.name == 'sc-catalog-dashboard') {
        console.log('already on some catalog dashboard...');
        return;
      }

      var catalogId = $injector.get('scCatalog').defCatalogId;
      if (catalogId) {
        $state.go('sc-catalog-dashboard', {catalogId: catalogId}, {location: 'replace'});
      }
    }

    function loadDomains() {
      $scope.domains = [];
      AuthDomain.query().then(
        function(domains) {
          $scope.samlDomain = domains.find(function(d) { return d.type == 'saml'; });

          var defaultDomain = $scope.global.appProps.default_domain;
          $scope.domains = domains;
          if (domains.length == 1) {
            $scope.loginData.domainName = domains[0].name;
          } else if (!!defaultDomain && domains.some(function(d) { return d.name == defaultDomain; })) {
            $scope.loginData.domainName = defaultDomain;
            if ($scope.samlDomain && $scope.samlDomain.name == defaultDomain) {
              // gotoIdp();
            }
          }
        }
      );
    }

    function gotoIdp() {
      $scope.showSignIn = false;

      if ($rootScope.reqState) {
        $window.localStorage['osReqState'] = JSON.stringify(angular.extend({time: new Date().getTime}, $rootScope.reqState));
      }

      $window.location.replace('saml/login?_nonce=' + Date.now());
    }

    function onLogin(loginReq, result) {
      $scope.loginError = false;

      if (result.status == "ok" && result.data && result.data.resetPasswordToken) {
        $state.go('reset-password', {token: result.data.resetPasswordToken, loginName: loginReq.loginName});
      } else if (result.status == "ok" && result.data) {
        $rootScope.currentUser = {
          id: result.data.id,
          firstName: result.data.firstName,
          lastName: result.data.lastName,
          loginName: result.data.loginName,
          admin: result.data.admin,
          daysBeforeExpiry: result.data.daysBeforeExpiry
        };
        $rootScope.loggedIn = true;
        AuthService.saveToken(result.data.token);
        if ($rootScope.reqState && $rootScope.state.name != $rootScope.reqState.name) {
          $state.go($rootScope.reqState.name, $rootScope.reqState.params);
          $rootScope.reqState = undefined;
        } else {
          $state.go('home');
        }
      } else {
        $rootScope.currentUser = {};
        $rootScope.loggedIn = false;
        AuthService.removeToken();
        $scope.loginError = true;
      }
    };

    $scope.onDomainSelect = function(domain) {
      if (domain.type == 'saml') {
        gotoIdp();
      }
    }

    $scope.login = function() {
      var samlDomain = $scope.samlDomain;
      var loginData  = $scope.loginData;

      if (samlDomain && samlDomain.name && samlDomain.name == loginData.domainName) {
        gotoIdp();
        return;
      }

      $scope.errors = [];
      AuthService.authenticate(loginData).then(
        function(resp) {
          onLogin(loginData, resp);
        },

        function(resp) {
          var errors = $scope.errors = (resp.data || []);
          for (var i = 0; i < errors.length; ++i) {
            var code = errors[i].code;
            if (code == 'USER_OTP_REQUIRED') {
              loginData.$$otpReq = true;
              errors[i].code = null;
              errors[i].message = $translate.instant('user.otp_required');
              break;
            }
          }

          Alerts.clear();
        }
      );
    }

    $scope.logout = function() {
      return AuthService.logout();
    }

    init();
  });
