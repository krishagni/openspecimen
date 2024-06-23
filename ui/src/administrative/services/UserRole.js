
import http from '@/common/services/HttpClient.js';

class UserRole {
  async getResources() {
    return http.get('rbac/resources');
  }

  async getRoles(filterOpts) {
    return http.get('rbac/roles', filterOpts || {});
  }

  async getRolesCount(filterOpts) {
    return http.get('rbac/roles/count', filterOpts || {});
  }

  async getRole(id) {
    return http.get('rbac/roles/' + id);
  }

  async saveOrUpdate(role) {
    if (!role.id) {
      return http.post('rbac/roles', role);
    } else {
      return http.put('rbac/roles/' + role.id, role);
    }
  }
}

export default new UserRole();
