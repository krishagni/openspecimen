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
}

export default new QueryFolder();
