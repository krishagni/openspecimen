import http from '@/common/services/HttpClient.js';

class QueryFolder {
  getFolders() {
    return http.get('query-folders', {});
  }

  getFolderById(folderId, includeQueries) {
    return http.get('query-folders/' + folderId, {includeQueries});
  }

  saveOrUpdate(folder) {
    if (folder.id > 0) {
      return http.put('query-folders/' + folder.id, folder);
    } else {
      return http.post('query-folders', folder);
    }
  }

  deleteFolder(folder) {
    return http.delete('query-folders/' + folder.id);
  }

  addQueriesToFolder(folder, queries) {
    const queryIds = (queries || []).map(query => query.id);
    return http.put('query-folders/' + folder.id + '/saved-queries', queryIds, {operation: 'ADD'});
  }

  rmQueriesFromFolder(folder, queries) {
    const queryIds = (queries || []).map(query => query.id);
    return http.put('query-folders/' + folder.id + '/saved-queries', queryIds, {operation: 'REMOVE'});
  }
}

export default new QueryFolder();
