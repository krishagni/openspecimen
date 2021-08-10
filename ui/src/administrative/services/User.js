
import http from '@/common/services/HttpClient.js';

class User {

  async getUserById(userId) {
    return http.get('users/' + userId);
  }

  async getUsers(filterOpts, pageOpts) {
    let params = Object.assign({}, filterOpts || {});
    params = Object.assign(params, pageOpts || {});
    return http.get('users', params);
  }

  async getUsersCount(filterOpts) {
    return http.get('users/count', filterOpts || {});
  }

  async saveOrUpdate(user) {
    if (!user.id) {
      return http.post('users', user);
    } else {
      return http.put('users/' + user.id, user);
    }
  }

  async bulkUpdate({detail, ids}) {
    if (!ids || ids.length == 0) {
      return [];
    }

    return http.put('users/bulk-update', {detail: detail, ids: ids});
  }

  async broadcast(announcement) {
    return http.post('users/announcements', announcement);
  }

  async updatePassword(input) {
    return http.put('users/password', input);
  }
}

export default new User();
