angular.module('os.common.search.service', [])
  .factory('QuickSearchSvc', function($translate, $http, ApiUrls) {
    var entitySearchMap = {}

    function register(entityName, searchOpts) {
      if (entitySearchMap[entityName]) {
        return;
      }

      entitySearchMap[entityName] = searchOpts;
    }

    function search(term) {
      return $http.get(ApiUrls.getBaseUrl() + 'search', {params: {term: term}}).then(
        function(resp) {
          var result = resp.data;
          angular.forEach(result, setMatchCaption);
          if (result.length == 0) {
            result.push({id: 0, key: term, caption: $translate.instant('search.user_manual', {term: term})});
          } else if (result.length > 20) {
            result.unshift({id: -2, caption: $translate.instant('search.many_matches')});
          }

          return result;
        }
      );
    }

    function setMatchCaption(match) {
      var opts = entitySearchMap[match.entity];
      match.group = $translate.instant(opts && opts.caption);
      match.caption = match.value;
      match.props = null;
      angular.forEach(match.entityProps,
        function(value) {
          match.props = match.props || '';
          match.props += (!!match.props ?  ', ' : '') + value;
        }
      );
    }

    function getState(entity) {
      return entitySearchMap[entity] && entitySearchMap[entity].state;
    }

    function getVueView(entity) {
      return entitySearchMap[entity] && entitySearchMap[entity].vueView;
    }

    return {
      register: register,

      search: search,

      getState: getState,

      getVueView: getVueView
    };
  });
