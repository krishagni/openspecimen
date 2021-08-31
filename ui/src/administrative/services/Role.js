
import http from '@/common/services/HttpClient.js';

class Role {

  async getRoles(filterOpts) {
    return http.get('rbac/roles', filterOpts || {});
  }

}

export default new Role();
