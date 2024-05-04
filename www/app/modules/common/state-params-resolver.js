angular.module('openspecimen')
  .factory('ObjectStateParamsResolver', function($http, ApiUrls) {
    return {
      getParams: function(objectName, key, value) {
        var params = { objectName: objectName, key: key, value: value };
        return $http.get(ApiUrls.getBaseUrl() + '/object-state-params', {params: params}).then(
          function(resp) {
            return resp.data;
          }
        );
      }
    }
  })
  .config(function($stateProvider) {
    $stateProvider.state('object-state-params-resolver', {
      url: '/object-state-params-resolver?:stateName&:objectName&:key&:value&:vueView',
      controller: function($state, $stateParams, params, VueApp, SettingUtil, QuickSearchSvc) {
        params.vueView = QuickSearchSvc.getVueView($stateParams.objectName);
        if (params.vueView) {
          VueApp.setVueView('resolve-view', params);
        } else {
          $state.go($stateParams.stateName || params.stateName, params, {location: 'replace'});
        }
      },
      resolve: {
        params: function($stateParams, ObjectStateParamsResolver) {
          return ObjectStateParamsResolver.getParams($stateParams.objectName, $stateParams.key, $stateParams.value);
        }
      },
      parent: 'signed-in'
    })
    .state('url-resolver', {
      url: '/resolve-url?:urlKey&:entityId',
      controller: function($state, $stateParams, $location, UrlResolver) {
        var state = UrlResolver.getStateParams($stateParams.urlKey, $stateParams.entityId);
        if (state) {
          $state.go(state.name, state.params, {location: 'replace'});
        } else {
          $state.go('home');
        }
      },
      parent: 'signed-in'
    });
  });
