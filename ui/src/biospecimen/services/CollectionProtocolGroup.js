
import http from '@/common/services/HttpClient.js';

class CollectionProtocolGroup {
  getGroups(opts) {
    return http.get('collection-protocol-groups', opts || {});
  }

  getGroupsCount(opts) {
    return http.get('collection-protocol-groups/count', opts || {});
  }

  getGroupById(id) {
    return http.get('collection-protocol-groups/' + id);
  }
}

export default new CollectionProtocolGroup();
