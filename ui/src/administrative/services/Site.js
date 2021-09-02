
import http from '@/common/services/HttpClient.js';

class Site {
  async getSites(filterOpts) {
    return http.get('sites', filterOpts || {});
  }

  async getSitesCount(filterOpts) {
    return http.get('sites/count', filterOpts || {});
  }

  async bulkUpdate({detail, ids}) {
    if (!ids || ids.length == 0) {
      return [];
    }

    return http.put('sites/bulk-update', {detail: detail, ids: ids});
  }
}

export default new Site();
