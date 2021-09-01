
import http from '@/common/services/HttpClient.js';

class UserGroup {

  async getUserGroups(filterOpts, pageOpts) {
    let params = Object.assign({}, filterOpts || {});
    params = Object.assign(params, pageOpts || {});
    return http.get('user-groups', params);
  }

  async getGroupsCount(filterOpts) {
    return http.get('user-groups/count', filterOpts || {});
  }

  async getUserGroup(id) {
    return http.get('user-groups/' + id);
  }

  async deleteGroup(group) {
    return http.delete('user-groups/' + group.id);
  }

  async addUsers(group, users) {
    let payload = users.map(u => ({id: u.id}));
    return http.put('user-groups/' + group.id + '/users', payload, {op: 'ADD'});
  }

  async removeUsers(group, users) {
    let payload = users.map(u => ({id: u.id}));
    return http.put('user-groups/' + group.id + '/users', payload, {op: 'REMOVE'});
  }
}

export default new UserGroup();
