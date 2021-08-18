
import http from '@/common/services/HttpClient.js';

class CollectionProtocol {

  async getCps(filterOpts) {
    return http.get('collection-protocols', filterOpts || {});
  }

}

export default new CollectionProtocol();

