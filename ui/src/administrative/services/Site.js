
import http from '@/common/services/HttpClient.js';

class Site {
  async getSites(filterOpts) {
    return http.get('sites', filterOpts || {});
  }

  async getSitesCount(filterOpts) {
    return http.get('sites/count', filterOpts || {});
  }

  async getSite(id) {
    return http.get('sites/' + id);
  }

  async saveOrUpdate(site) {
    if (!site.id) {
      return http.post('sites', site);
    } else {
      return http.put('sites/' + site.id, site);
    }
  }

  async bulkUpdate({detail, ids}) {
    if (!ids || ids.length == 0) {
      return [];
    }

    return http.put('sites/bulk-update', {detail: detail, ids: ids});
  }
}

export default new Site();
