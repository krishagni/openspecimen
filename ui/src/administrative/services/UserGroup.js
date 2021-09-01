
import http from '@/common/services/HttpClient.js';

class UserGroup {

  getUserGroups(filterOpts, pageOpts) {
    let params = Object.assign({}, filterOpts || {});
    params = Object.assign(params, pageOpts || {});
    return http.get('user-groups', params);
  }

  getGroupsCount(filterOpts) {
    return http.get('user-groups/count', filterOpts || {});
  }

  getUserGroup(id) {
    return http.get('user-groups/' + id);
  }

  addUsers(group, users) {
    let payload = users.map(u => ({id: u.id}));
    return http.put('user-groups/' + group.id + '/users', payload, {op: 'ADD'});
  }

  removeUsers(group, users) {
    let payload = users.map(u => ({id: u.id}));
    return http.put('user-groups/' + group.id + '/users', payload, {op: 'REMOVE'});
  }
}

export default new UserGroup();
