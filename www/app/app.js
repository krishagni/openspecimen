'use strict';

var osApp = angular.module('openspecimen', [
  'os.common',
  'os.biospecimen',
  'os.administrative',
  'os.query',

  'ngMessages',
  'ngCookies',
  'ngSanitize', 
  'ngGrid',
  'ui.router', 
  'ui.bootstrap', 
  'ui.mask', 
  'ui.keypress', 
  'ui.select',
  'ui.sortable',
  'ui.autocomplete',
  'mgcrea.ngStrap.popover',
  'angular-loading-bar',
  'pascalprecht.translate',
  'chart.js',
  'ui.tinymce'
  ]);

osApp.config(function(
    $stateProvider, $urlRouterProvider, $httpProvider, $controllerProvider,
    $compileProvider, $filterProvider, $provide, $locationProvider,
    $translateProvider, $translatePartialLoaderProvider,
    uiSelectConfig, ApiUrlsProvider) {

    $locationProvider.hashPrefix('');

    osApp.providers = {
      controller : $controllerProvider.register,
      state      : $stateProvider.state,
      directive  : $compileProvider.directive,
      filter     : $filterProvider.register,
      factory    : $provide.factory,
      service    : $provide.service
    };

    $translatePartialLoaderProvider.addPart('modules');
    $translateProvider.useSanitizeValueStrategy(null);
    $translateProvider.useLoader('$translatePartialLoader', {  
      urlTemplate: '{part}/i18n/{lang}.js',
      loadFailureHandler: 'i18nErrHandler'
    });
 
    $stateProvider
      .state('default', {
        abstract: true,
        templateUrl: 'modules/common/default.html',
        controller: function($scope, Alerts) {
          $scope.alerts = Alerts.messages;
        }
      })
      .state('alert-msg', {
        url: '/alert?type&msg',
        views: {
          'nav-buttons': {
            template: '<div></div>',
            controller: function() { }
          },

          'app-body': {
            template: '<div class="os-center-box" style="margin-top: 100px;">' +
                      '  <div class="alert alert-{{actx.type}}">{{actx.msg}}</div>' +
                      '  <button type="submit" class="btn btn-block btn-primary" ui-sref="login">' +
                      '    <span translate="user.sign_in">Sign in</span>' +
                      '  </button>' +
                      '</div>',
            controller: function($scope, $stateParams) {
              $scope.actx = {type: $stateParams.type, msg: $stateParams.msg};
            }
          }
        },
        parent: 'default'
      })
      .state('signed-in', {
        abstract: true,
        templateUrl: 'modules/common/appmenu.html',
        resolve: {
          currentUser: function(User) {
            return User.getCurrentUser();
          },
          userUiState: function(User) {
            return User.getUiState();
          },
          authInit: function(currentUser, AuthorizationService) {
            return AuthorizationService.initializeUserRights(currentUser);
          },
          videoSettings : function (Setting) {
            return Setting.getWelcomeVideoSetting();
          },
          authToken: function(userUiState, AuthService) {
            if (userUiState.authToken) {
              AuthService.saveToken(userUiState.authToken);
            }
          },
          translationsReady: function($translate) {
            return $translate.onReady().then(
              function() {
                return $translate.isReady();
              }
            );
          }
        },
        controller: 'SignedInCtrl'
      })
      .state('headless-view', {
        url: '?token',
        abstract: true,
        templateUrl: 'modules/common/headless-view.html',
        controller: function($scope, Alerts) {
          $scope.alerts = Alerts.messages;
        },
        resolve: {
          initToken: function($stateParams, $http, $window, AuthService) {
            if ($stateParams.token) {
              $http.defaults.headers.common['X-OS-API-TOKEN'] =
                $window.localStorage['osAuthToken'] =
                  $stateParams.token;
              return AuthService.refreshCookie();
            }

            return null;
          },

          currentUser: function(initToken, User) {
            if (initToken) {
              return User.getCurrentUser();
            }

            return null;
          },

          authInit: function(currentUser, AuthorizationService) {
            return AuthorizationService.initializeUserRights(currentUser);
          },

          userUiState: function(initToken, User) {
            if (initToken) {
              return User.getUiState();
            }

            return null;
          },

          authToken: function(userUiState, AuthService) {
            if (userUiState && userUiState.authToken) {
              AuthService.saveToken(userUiState.authToken);
            }
          }
        }
      })
      .state('home', {
        url: '/home',
        template: '<div></div>',
        controller: function(VueApp) { VueApp.setVueView('home'); },
        parent: 'signed-in'
      })
      .state('admin-view', {
        abstract: true,
        template: '<div ui-view></div>',
        resolve: {
          isAdmin: function(currentUser, Util) {
            return Util.booleanPromise(currentUser.admin);
          }
        },
        parent: 'signed-in'
      })
      .state('vue-view', {
        url: '/vue-view?url&relative',
        template: '<div ui-view></div>',
        controller: function($state, $stateParams, $sce, $rootScope, authInit, translationsReady, VueApp) {
          if (!$stateParams.url) {
            $state.go('home');
          } else {
            var url = $stateParams.url;
            if ($stateParams.relative == 'true') {
              url = VueApp.getVueViewUrl(url);
            }

            window.location.href = $rootScope.vueUrl = $sce.trustAsResourceUrl(url);
          }
        },
        parent: 'signed-in'
      });

    $urlRouterProvider.otherwise('/');

    $httpProvider.interceptors.push('httpRespInterceptor');

    ApiUrlsProvider.urls = {
      'sessions': 'rest/ng/sessions',
      'sites': 'rest/ng/sites',
      'form-files': 'rest/ng/form-files'
    };

    uiSelectConfig.theme = 'bootstrap';
  })
  .factory('i18nErrHandler', function($q) {
    return function(part, lang) {
      return $q.when({});
    }
  })
  .factory('httpRespInterceptor', function(
    $rootScope, $q, $injector, $location, $window, $templateCache, $timeout,
    Alerts, LocationChangeListener) {

    var qp = '?_buildVersion=' + ui.os.appProps.build_version + '&_buildDate=' + ui.os.appProps.build_date;

    function displayErrMsgs(errors) {
      var errMsgs = errors.map(
        function(err) {
          return err.message + " (" + err.code + ")";
        }
      );
      Alerts.errorText(errMsgs);
    }

    var totalReqs = 0;

    var completedReqs = 0;

    var listeners = [];

    var timerId = undefined;

    var fourNotOneTimer = undefined;

    function notifyListeners() {
      if (timerId) {
        clearTimeout(timerId);
      }

      timerId = setTimeout(notifyListeners0, 500);
    }

    function notifyListeners0() {
      if (completedReqs >= totalReqs) {
        angular.forEach(listeners,
          function(listener) {
            listener();
          }
        );

        listeners = [];
        totalReqs = completedReqs = 0;
      }
    }

    function addListener(listener) {
      if (completedReqs >= totalReqs) {
        listener();
      } else {
        listeners.push(listener);
      }
    }

    function isHtmlReq(config) {
      return config.method == 'GET' &&
        (config.url.substr(0, 7) == 'modules' || config.url.substr(0, 19) == 'plugin-ui-resources');
    }

    return {
      request: function(config) {
        if (!isHtmlReq(config)) {
          ++totalReqs;
        }

        if (config.method == 'GET' && $templateCache.get(config.url) == null) {
          if (config.url.indexOf('modules') == 0 || config.url.indexOf('plugin-ui-resources') == 0) {
            config.url += qp;
          }
        }

        return config || $q.when(config);
      },

      requestError: function(rejection) {
        $q.reject(rejection);
      },

      response: function(response) {
        var apiCall = !isHtmlReq(response.config);
        if (apiCall) {
          ++completedReqs;
        }

        var httpMethods = ['POST', 'PUT', 'PATCH'];
        if (response.status == 200 && httpMethods.indexOf(response.config.method) != -1) {
          LocationChangeListener.allowChange();
        }

        if (apiCall) {
          notifyListeners();
        }

        return response || $q.when(response);
      },

      responseError: function(rejection) {
        var apiCall = !isHtmlReq(rejection.config);
        if (apiCall) {
          ++completedReqs;
        }

        if (rejection.status == 0) {
          Alerts.error("common.server_connect_error");
        } else if (rejection.status == 401) {
          $rootScope.loggedIn = false;

          delete $window.localStorage['osAuthToken'];
          delete $injector.get("$http").defaults.headers.common['X-OS-API-TOKEN'];

          if (fourNotOneTimer) {
            clearTimeout(fourNotOneTimer);
          }

          fourNotOneTimer = setTimeout(
            function() {
              $injector.get('$state').go('login'); // using injector to get rid of circular dependencies
            }, 1000);

          // $timeout(function() { $window.location.reload(); }, 1000);

          // $location.path('/');
          // $timeout(function() { $window.location.reload(); }, 1000);
        } else if (rejection.status / 100 == 5) {
          if (rejection.data instanceof Array) {
            displayErrMsgs(rejection.data);
          } else {
            Alerts.error("common.server_error");
          }
        } else if (rejection.status / 100 == 4) {
          if (rejection.data instanceof Array) {
            displayErrMsgs(rejection.data);
          } else if (rejection.config.method != 'HEAD') {
            Alerts.error('common.ui_error');
          }
        } 

        if (apiCall) {
          notifyListeners();
        }

        return $q.reject(rejection);
      },

      addListener: addListener
    };
  })
  .factory('ApiUtil', function($window, $http) {
    return {
      processResp: function(result) {
        var response = {};
        if (result.status / 100 == 2) {
          response.status = "ok";
        } else if (result.status / 100 == 4) {
          response.status = "user_error";
        } else if (result.status / 100 == 5) {
          response.status = "server_error";
        }

        response.data = result.data;
        return response;
      },

      initialize: function(token) {
        $http.defaults.headers.common['X-OS-API-CLIENT'] = "webui";

        if (!token) {
          token = $window.localStorage['osAuthToken'];
          if (!token) {
            return;
          }
        }

        $http.defaults.headers.common['X-OS-API-TOKEN'] = token;
        $http.defaults.withCredentials = true;
      }
    };
  })
  .provider('ApiUrls', function() {
    var that = this;
    var server = ui.os.server;

    this.urls = {};

    this.$get = function() {
      return {
        hostname: server.hostname,
        port    : server.port,
        secure  : server.secure,
        app     : server.app,
        urls    : that.urls,

        getServerUrl: function() {
          return server.url;
        },

        getBaseUrl: function() {
          return server.url + 'rest/ng/';
        },

        getUrl: function(key) {
          var url = '';
          if (key) {
            url = this.urls[key];
          }
          return server.url + url;
        }
      };
    }
  })
  .factory('VueApp', function($sce, $rootScope, $window) {
    var baseUrl = 'ui-app/#/';
    // var baseUrl = 'http://localhost:8081/#/';

    function getVueViewUrl(state, params) {
      var url = baseUrl + state;
      var query = '';
      angular.forEach(params,
        function(value, key) {
          if (value) {
            if (query) {
              query += '&';
            }

            query += key + '=' + value;
          }
        }
      );

      if (baseUrl.indexOf('http') == 0) {
        if (query) {
          query += '&';
        }

        query += 'token=' + $window.localStorage['osAuthToken'];
      }

      if (query) {
        if (url.indexOf('?') == -1) {
          url += '?';
        } else {
          url += '&';
        }

        url += query;
      }

      return url;
    }

    function setVueView(state, params) {
      var url = getVueViewUrl(state, params);
      window.location.href = $rootScope.vueUrl = $sce.trustAsResourceUrl(url);
    }

    return {
      getVueViewUrl: getVueViewUrl,

      setVueView: setVueView
    }
  })
  .run(function(
    $rootScope, $window, $document, $http, $cookies, $q,  $state, $translate, $translatePartialLoader, $sce, $timeout,
    AuthService, AuthorizationService, HomePageSvc, LocationChangeListener,
    ApiUtil, Setting, PluginReg, Util, ItemsHolder, VueApp) {

    function isRedirectAllowed(st) {
      return !st.data || st.data.redirect !== false;
    }

    function isLandingView(st) {
      return st.data && (st.data.landingView === true);
    }

    if (!angular.merge) {
      angular.merge = function(dst, src) {
        return Util.merge(src, dst);
      }
    }

    $document.on('click', '.dropdown-menu.dropdown-menu-form', function(e) {
      e.stopPropagation();
    });

    $http.defaults.headers.common['X-OS-API-CLIENT'] = "webui";
    $http.defaults.headers.common['X-OS-CLIENT-TZ'] = Intl.DateTimeFormat().resolvedOptions().timeZone;
    $http.defaults.withCredentials = true;

    if ($window.localStorage['osAuthToken']) {
      $http.defaults.headers.common['X-OS-API-TOKEN'] = $window.localStorage['osAuthToken'];
      AuthService.refreshCookie();
      $rootScope.loggedIn = true;
    } else if ($cookies['osAuthToken']) {
      $window.localStorage['osAuthToken'] = $cookies['osAuthToken'];
      $http.defaults.headers.common['X-OS-API-TOKEN'] = $cookies['osAuthToken'];
      $rootScope.loggedIn = true;
    }

    ui.os.global = $rootScope.global = {
      defaultDomain: 'openspecimen',
      filterWaitInterval: ui.os.appProps.searchDelay,
      appProps: ui.os.appProps
    };

    $rootScope.$on('$stateChangeSuccess',
      function(event, toState, toParams, fromState, fromParams) {
        $rootScope.state = toState;
      }
    );

    $rootScope.$on('$stateChangeStart',
      function(event, toState, toParams, fromState, fromParams) {
        LocationChangeListener.onChange(event);

        if (toState.parent != 'default-nav-buttons' && !isLandingView(toState)) {
          $rootScope.reqState = {
            name: toState.name,
            params: toParams
          };
        } else if ($rootScope.loggedIn && isRedirectAllowed(toState)) {
          event.preventDefault();
          if ($rootScope.reqState) {
            $state.go($rootScope.reqState.name, $rootScope.reqState.params);
          } else {
            $state.go("home");
          }
        }

        $rootScope.stateChangeInfo = {
          toState  : toState,   toParams  : toParams,
          fromState: fromState, fromParams: fromParams
        };
      }
    );
      
    $rootScope.$on('$stateChangeError',
      function(event, toState, toParams, fromState, fromParams) {
        if ($rootScope.currentUser && !fromState.name) {
          $state.go('home');
        } else if (fromState.name) {
          $state.go(fromState.name);
        }
      });

    $rootScope.includesState = function(stateName, params, options) {
      return $state.includes(stateName, params, options);
    }

    $rootScope.back = LocationChangeListener.back;
    var loadLocale = $rootScope.loadLocale = function() {
      return Setting.getLocale().then(
        function(localeSettings) {
          angular.extend(
            $rootScope.global,
            {
              dateFmt: localeSettings.dateFmt,
              shortDateFmt: localeSettings.deBeDateFmt,
              timeFmt: localeSettings.timeFmt,
              queryDateFmt: {format: localeSettings.deFeDateFmt},
              dateTimeFmt: localeSettings.dateFmt + ' ' + localeSettings.timeFmt,
              locale: localeSettings.locale,
              utcOffset: localeSettings.utcOffset
            }
          );

          return localeSettings;
        }
      );
    }

    loadLocale().then(
      function(localeSettings) {
        var plugins = ui.os.appProps.plugins;
        PluginReg.usePlugins(plugins);
        angular.forEach(plugins, function(plugin) {
          $translatePartialLoader.addPart('plugin-ui-resources/'+ plugin + '/');
        });
        
        var locale = localeSettings.locale;
        $translate.use(locale);

        var idx = locale.indexOf('_');
        var localeLang = (idx != -1) ? locale.substring(0, idx) : locale;

        var langs = [locale];
        if (locale !== localeLang) {
          langs.push(localeLang);
        }

        if (localeLang != 'en') {
          langs.push('en');
        }

        $translate.fallbackLanguage(langs);
        $translate.refresh();
      }
    );

    Setting.getDeploymentSiteAssets().then(
      function(resp) {
        $rootScope.global.siteAssets = resp;
      }
    );

    var mediaQuery = $window.matchMedia(
      'only screen and ' +
      '(min-device-width: 375px) and ' +
      '(max-device-width: 812px)'
    );

    function setScreenType(query) {
      $rootScope.mobileScreen = query.matches ? true : false;
    }

    setScreenType(mediaQuery);
    mediaQuery.addEventListener('change', function() { $timeout(function() { setScreenType(mediaQuery); }, 0); });
  });
