
import http from '@/common/services/HttpClient.js';

class Site {

  async getSites(filterOpts) {
    return http.get('sites', filterOpts || {});
  }

  async getSitesCount(filterOpts) {
    return http.get('sites/count', filterOpts || {});
  }
}

export default new Site();
