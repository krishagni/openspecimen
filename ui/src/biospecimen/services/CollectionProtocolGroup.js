
import http from '@/common/services/HttpClient.js';

class CollectionProtocolGroup {
  getGroups(opts) {
    return http.get('collection-protocol-groups', opts || {});
  }

  getGroupsCount(opts) {
    return http.get('collection-protocol-groups/count', opts || {});
  }

  getGroupById(id) {
    return http.get('collection-protocol-groups/' + id);
  }

  getForms(groupId, level) {
    return http.get('collection-protocol-groups/' + groupId + '/forms', {level})
  }

  addForms(groupId, level, forms) {
    const payload = {level, forms};
    return http.post('collection-protocol-groups/' + groupId + '/forms', payload);
  }

  removeForms(groupId, level, formIds) {
    const formIdQp = formIds.map(formId => 'formId=' + formId).join('&');
    const qp = 'level=' + level + '&' + formIdQp;
    return http.delete('collection-protocol-groups/' + groupId + '/forms?' + qp);
  }
}

export default new CollectionProtocolGroup();
