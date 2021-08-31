
import http from '@/common/services/HttpClient.js';

class Site {

  async getSites(filterOpts) {
    return http.get('sites', filterOpts || {});
  }

}

export default new Site();
