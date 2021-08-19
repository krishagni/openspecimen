
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

  async delete(user) {
    return http.delete('users/' + user.id);
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

  async updateStatus(user, status) {
    return http.put('users/' + user.id + '/activity-status', {activityStatus: status});
  }

  async getDependents(user) {
    return http.get('users/' + user.id + '/dependent-entities');
  }

  async impersonate(user) {
    return http.post('sessions/impersonate', {userId: user.id});
  }

  async getRoles(user) {
    return http.get('rbac/subjects/' + user.id + '/roles');
  }

  async updateRole(user, role) {
    if (role.id) {
      return http.put('rbac/subjects/' + user.id + '/roles/' + role.id, role);
    } else {
      return http.post('rbac/subjects/' + user.id + '/roles', role);
    }
  }

  async deleteRole(user, role) {
    return http.delete('rbac/subjects/' + user.id + '/roles/' + role.id);
  }

  async getForms(user) {
    return http.get('users/' + user.id + '/forms');
  }

  async getFormRecords(user) {
    return http.get('users/' + user.id + '/form-records').then(
      (frs) => {
        frs.forEach(
          (formRecords) =>
            formRecords.records.forEach(
              (record) => {
                record.formId = formRecords.id;
                record.formCaption = formRecords.caption;
              }
            )
        );

        return frs;
      }
    );
  }
}

export default new User();
