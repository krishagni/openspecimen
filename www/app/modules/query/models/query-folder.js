
angular.module('os.query.models.queryfolder', ['os.common.models'])
  .factory('QueryFolder', function($http, osModel, SavedQuery) {
    var QueryFolder = osModel('query-folders');

    QueryFolder.prototype.query = function(filterOpts) {
      return $http.get(QueryFolder.url() + this.$id() + '/saved-queries', {params: filterOpts}).then(
        function(resp) {
          return resp.data.queries.map(
            function(query) {
              return new SavedQuery(query);
            }
          );
        }
      );
    };

    QueryFolder.prototype.getQueries = function(filterOpts) {
      var queryList = {count: 0, queries: []};
      var params = angular.extend({countReq: false}, filterOpts);

      $http.get(QueryFolder.url() + this.$id() + '/saved-queries', {params: params}).then(
        function(resp) {
          queryList.count = resp.data.count;
          queryList.queries = resp.data.queries.map(
            function(query) {
              return new SavedQuery(query);
            }
          );
        }
      );
       
      return queryList;
    };

    QueryFolder.prototype.getCount = function(filterOpts) {
      return $http.get(QueryFolder.url() + this.$id() + '/saved-queries-count', {params: filterOpts}).then(
        function(resp) {
          return resp.data;
        }
      );
    };

    QueryFolder.prototype.addQueries = function(queries) {
      var queryIds = queries.map(function(query) { return query.id });
      return $http.put(addQueriesUrl(this.$id()), queryIds).then(
        function(resp) {
          return resp.data;
        }
      );
    };

    function addQueriesUrl(folderId) {
      return QueryFolder.url() + folderId + '/saved-queries?operation=ADD'; 
    };

    return QueryFolder;
  });
