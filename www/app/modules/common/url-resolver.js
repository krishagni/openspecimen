
angular.module('os.common')
  .factory('UrlResolver', function($state) {
    var urlStateMap = {};

    function regUrlState(urlKey, stateName, idName) {
      urlStateMap[urlKey] = {stateName: stateName, idName: idName};
    }

    function getUrl(urlKey, idValue) {
      var state = getStateParams(urlKey, idValue);
      return state ? $state.href(state.name, state.params) : null;
    }

    function getStateParams(urlKey, idValue) {
      var urlState = urlStateMap[urlKey];
      if (!urlState) {
        return null;
      }

      var params = {};
      params[urlState.idName || 'id'] = idValue;
      return {name: urlState.stateName, params: params};
    }

    return {
      regUrlState: regUrlState,

      getUrl: getUrl,

      getStateParams: getStateParams
    };
  }
);
