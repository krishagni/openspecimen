import http from '@/common/services/HttpClient.js';

class SavedQuery {
  getQueries(opts) {
    opts = opts || {};
    return http.get('saved-queries', {returnList: true, ...opts}); 
  }

  getQueriesCount(opts) {
    return http.get('saved-queries/count', opts || {});
  }

  getQueryById(queryId) {
    return http.get('saved-queries/' + queryId);
  }

  downloadQuery(queryId) {
    return http.downloadFile(http.getUrl('saved-queries/' + queryId + '/definition-file'));
  }

  saveOrUpdate(query) {
    if (query.id > 0) {
      return http.put('saved-queries/' + query.id, query);
    } else {
      return http.post('saved-queries', query);
    }
  }

  deleteQuery(queryId) {
    return http.delete('saved-queries/' + queryId);
  }

  star(queryId) {
    return http.post('saved-queries/' + queryId + '/labels', {});
  }

  unstar(queryId) {
    return http.delete('saved-queries/' + queryId + '/labels');
  }
}

export default new SavedQuery();
